package com.example.exile_overlay.client.sound;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCStdlib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MP3 / OGG オーディオファイルをデコードし、OpenALで直接再生するプレイヤー。
 * ソフトウェアゲイン増幅により 0%〜200%（2.0f）の音量ブーストに対応。
 * デコードは別スレッドで行い、全OpenAL操作はメインスレッドへ委譲する。
 */
@OnlyIn(Dist.CLIENT)
public class ExileAudioPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ExileAudioPlayer");
    private static final int MAX_FRAMES = 8000; // ~3分相当

    private record DecodeResult(byte[] pcm, int sampleRate, int channels) {}

    /**
     * オーディオファイル（.ogg / .mp3）を指定音量で再生する。メインスレッドから呼び出すこと。
     * volume は 0.0f〜2.0f（1.0f = 100%）。
     */
    public static void playCustomSound(File file, float volume) {
        if (file == null || !file.exists()) {
            LOGGER.warn("音声ファイルが見つかりません: {}", file);
            return;
        }
        if (volume <= 0.0f) {
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                String name = file.getName().toLowerCase(Locale.ROOT);
                DecodeResult result = name.endsWith(".ogg") ? decodeOgg(file) : decodeMp3(file);
                if (result == null) return;

                byte[] amplifiedPcm = applyVolume(result.pcm(), volume);
                DecodeResult finalResult = new DecodeResult(amplifiedPcm, result.sampleRate(), result.channels());

                net.minecraft.client.Minecraft.getInstance().tell(() -> playPcm(finalResult));
            } catch (Exception e) {
                LOGGER.error("[exile_overlay] 音声再生失敗: {}", file.getName(), e);
            }
        }, "ExileOverlay-AudioDecoder");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 後方互換用メソッド。
     */
    public static void playMp3(File file, float volume) {
        playCustomSound(file, volume);
    }

    /**
     * STBVorbis を使用して OGG をデコードして 16bit PCM を返す。
     */
    private static DecodeResult decodeOgg(File file) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer pcmBuffer = STBVorbis.stb_vorbis_decode_filename(file.getAbsolutePath(), channels, sampleRate);
            if (pcmBuffer == null) {
                LOGGER.error("OGGデコード失敗 (STBVorbis): {}", file.getName());
                return null;
            }

            try {
                int ch = channels.get(0);
                int rate = sampleRate.get(0);
                int sampleCount = pcmBuffer.remaining();
                byte[] pcmBytes = new byte[sampleCount * 2];
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmBuffer);
                return new DecodeResult(pcmBytes, rate, ch);
            } finally {
                LibCStdlib.free(pcmBuffer);
            }
        } catch (Exception e) {
            LOGGER.error("OGGデコード例外: {}", file.getName(), e);
            return null;
        }
    }

    /**
     * JLayer で MP3 をデコードして 16bit PCM を返す。
     */
    private static DecodeResult decodeMp3(File file) {
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            Bitstream bitstream = new Bitstream(is);
            Decoder decoder = new Decoder();
            List<byte[]> chunks = new ArrayList<>();
            int sampleRate = 44100;
            int channels = 2;
            int frames = 0;

            Header header;
            while ((header = bitstream.readFrame()) != null && frames < MAX_FRAMES) {
                if (frames == 0) {
                    sampleRate = header.frequency();
                    channels = (header.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;
                }
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                int count = output.getBufferLength();
                short[] samples = output.getBuffer();

                byte[] chunk = new byte[count * 2];
                ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples, 0, count);
                chunks.add(chunk);

                bitstream.closeFrame();
                frames++;
            }

            int totalBytes = chunks.stream().mapToInt(b -> b.length).sum();
            byte[] result = new byte[totalBytes];
            int offset = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, result, offset, chunk.length);
                offset += chunk.length;
            }
            return new DecodeResult(result, sampleRate, channels);

        } catch (Exception e) {
            LOGGER.error("MP3デコード失敗: {}", file.getName(), e);
            return null;
        }
    }

    /**
     * 16bit PCM サンプルに対してソフトウェアゲイン増幅（クリッピング保護付き）を適用する。
     */
    private static byte[] applyVolume(byte[] pcm, float volume) {
        if (pcm == null || pcm.length == 0 || Math.abs(volume - 1.0f) < 0.001f) {
            return pcm;
        }
        if (volume <= 0.0f) {
            return new byte[pcm.length];
        }

        byte[] result = new byte[pcm.length];
        for (int i = 0; i < pcm.length - 1; i += 2) {
            int sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            int amplified = Math.round(sample * volume);
            if (amplified > Short.MAX_VALUE) {
                amplified = Short.MAX_VALUE;
            } else if (amplified < Short.MIN_VALUE) {
                amplified = Short.MIN_VALUE;
            }
            result[i] = (byte) (amplified & 0xFF);
            result[i + 1] = (byte) ((amplified >> 8) & 0xFF);
        }
        return result;
    }

    /**
     * PCMデータを OpenAL バッファへ流し込んで再生する。メインスレッドで呼ぶこと。
     */
    private static void playPcm(DecodeResult decoded) {
        int buffer = AL10.alGenBuffers();
        int source = AL10.alGenSources();

        try {
            ByteBuffer buf = ByteBuffer.allocateDirect(decoded.pcm().length);
            buf.put(decoded.pcm());
            buf.flip();

            int alFormat = decoded.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            AL10.alBufferData(buffer, alFormat, buf, decoded.sampleRate());
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);

            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(source, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
            AL10.alSource3f(source, AL10.AL_VELOCITY, 0.0f, 0.0f, 0.0f);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0.0f);
            AL10.alSourcef(source, AL10.AL_MAX_GAIN, 20.0f);
            AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);

            AL10.alSourcePlay(source);

            long durationMs = (long) decoded.pcm().length * 1000L
                    / ((long) decoded.sampleRate() * decoded.channels() * 2L) + 2000L;
            int finalSource = source;
            int finalBuffer = buffer;
            Thread cleanup = new Thread(() -> {
                try {
                    Thread.sleep(durationMs);
                } catch (InterruptedException ignored) {}
                net.minecraft.client.Minecraft.getInstance().tell(() -> {
                    AL10.alDeleteSources(finalSource);
                    AL10.alDeleteBuffers(finalBuffer);
                });
            }, "ExileOverlay-AudioCleanup");
            cleanup.setDaemon(true);
            cleanup.start();

        } catch (Exception e) {
            LOGGER.error("OpenAL再生エラー", e);
            AL10.alDeleteSources(source);
            AL10.alDeleteBuffers(buffer);
        }
    }
}
