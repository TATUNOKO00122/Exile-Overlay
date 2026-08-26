package com.example.exile_overlay.client.sound;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * JLayerでMP3をデコードし、OpenALで直接再生するプレイヤー。
 * デコードは別スレッドで行い、全OpenAL操作はメインスレッドへ委譲する。
 */
@OnlyIn(Dist.CLIENT)
public class ExileAudioPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ExileAudioPlayer");
    private static final int MAX_FRAMES = 8000; // ~3分相当

    private record DecodeResult(byte[] pcm, int sampleRate, int channels) {}

    /**
     * MP3ファイルを指定音量で再生する。メインスレッドから呼び出すこと。
     * volume は 0.0f〜20.0f（1.0f = 100%）。
     */
    public static void playMp3(File file, float volume) {
        if (file == null || !file.exists()) {
            LOGGER.warn("MP3ファイルが見つかりません: {}", file);
            return;
        }
        LOGGER.info("[exile_overlay] MP3再生要求: path='{}', volume={}", file.getName(), volume);

        Thread thread = new Thread(() -> {
            try {
                DecodeResult result = decodeMp3(file);
                if (result == null) return;
                // OpenAL コンテキストはメインスレッドが保持するため tell() で委譲
                net.minecraft.client.Minecraft.getInstance().tell(() -> playPcm(result, volume));
            } catch (Exception e) {
                LOGGER.error("[exile_overlay] MP3再生失敗: {}", file.getName(), e);
            }
        }, "ExileOverlay-Mp3Decoder");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * JLayer で MP3 をデコードして 16bit PCM を返す。
     * モノラル MP3 は channels=1、ステレオ/ジョイントステレオ等は channels=2。
     * JLayer の SampleBuffer はチャンネル数に応じたデータのみ出力する（モノ→1ch、ステレオ→2ch）。
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
                    // SINGLE_CHANNEL = 3 がモノラル。それ以外（STEREO/JOINT_STEREO/DUAL_CHANNEL）はステレオ扱い。
                    channels = (header.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;
                }
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                int count = output.getBufferLength();
                short[] samples = output.getBuffer();

                // short[] → byte[] (little-endian)
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
            LOGGER.info("[exile_overlay] MP3デコード完了: {} ({}フレーム, {}Hz, {}ch, {}bytes)", file.getName(), frames, sampleRate, channels, totalBytes);
            return new DecodeResult(result, sampleRate, channels);

        } catch (Exception e) {
            LOGGER.error("MP3デコード失敗: {}", file.getName(), e);
            return null;
        }
    }

    /**
     * PCMデータを OpenAL バッファへ流し込んで再生する。メインスレッドで呼ぶこと。
     * AL_GAIN は Minecraft の音量制限を受けないため volume > 1.0f が有効。
     */
    private static void playPcm(DecodeResult decoded, float volume) {
        int buffer = AL10.alGenBuffers();
        int source = AL10.alGenSources();

        try {
            ByteBuffer buf = ByteBuffer.allocateDirect(decoded.pcm().length);
            buf.put(decoded.pcm());
            buf.flip();

            // チャンネル数に応じた AL フォーマットを選択。
            // モノラルMP3 を STEREO16 で渡すと OpenAL が 2ch として解釈し 2倍速再生になるため必須。
            int alFormat = decoded.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            AL10.alBufferData(buffer, alFormat, buf, decoded.sampleRate());
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);

            // 2D/UIサウンドとして耳元で再生（リスナー相対座標 & 距離減衰なし）
            // これを設定しないとワールド座標(0,0,0)で再生され、原点から離れたプレイヤーには距離減衰で無音になる
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(source, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
            AL10.alSource3f(source, AL10.AL_VELOCITY, 0.0f, 0.0f, 0.0f);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0.0f);

            AL10.alSourcef(source, AL10.AL_GAIN, Math.max(0.0f, volume));
            AL10.alSourcePlay(source);

            // 再生時間 = bytes / (sampleRate × channels × 2bytes/sample)
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
            }, "ExileOverlay-Mp3Cleanup");
            cleanup.setDaemon(true);
            cleanup.start();

        } catch (Exception e) {
            LOGGER.error("OpenAL再生エラー", e);
            AL10.alDeleteSources(source);
            AL10.alDeleteBuffers(buffer);
        }
    }
}
