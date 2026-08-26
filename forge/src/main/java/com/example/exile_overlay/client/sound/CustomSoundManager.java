package com.example.exile_overlay.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import com.example.exile_overlay.ExileOverlayMod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = ExileOverlayMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class CustomSoundManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SOUND_DIR_NAME = "sounds";
    private static CustomSoundPack customSoundPack;
    private static File soundDir;

    public static void init() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(ExileOverlayMod.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        soundDir = new File(configDir, SOUND_DIR_NAME);
        if (!soundDir.exists()) {
            soundDir.mkdirs();
            LOGGER.info("Created custom sound directory at: {}", soundDir.getAbsolutePath());
        }
        
        customSoundPack = new CustomSoundPack("exile_overlay_custom_sounds", soundDir);
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {
            return;
        }
        
        if (customSoundPack == null) {
            init();
        }

        try {
            Pack pack = Pack.readMetaAndCreate(
                "exile_overlay_custom_sounds",
                Component.literal("Exile Overlay Custom Sounds"),
                true,
                (packId) -> customSoundPack,
                net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                Pack.Position.TOP,
                PackSource.BUILT_IN
            );
            
            if (pack != null) {
                event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
                LOGGER.info("Registered custom sound pack finder.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register custom sound pack", e);
        }
    }

    /**
     * リロード時に新しいファイルがないか確認する
     */
    public static void reloadSounds() {
        if (customSoundPack != null) {
            customSoundPack.reload();
            Minecraft.getInstance().getSoundManager().reload(); // Reload sounds in Minecraft
        }
    }

    /**
     * 専用フォルダ内の .ogg/.mp3 ファイル一覧を取得する。
     * 返す文字列は拡張子なしのファイル名。MP3 は末尾に ".mp3" マーカーを付けて区別する。
     */
    public static List<String> getAvailableCustomSounds() {
        List<String> list = new ArrayList<>();
        if (soundDir == null || !soundDir.exists() || !soundDir.isDirectory()) return list;

        File[] files = soundDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".ogg") || lower.endsWith(".mp3");
        });
        if (files == null) return list;

        for (File file : files) {
            String name = file.getName();
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".ogg")) {
                list.add(name.substring(0, name.length() - 4));
            } else if (lower.endsWith(".mp3")) {
                // MP3 は "name.mp3" という識別子で返す
                list.add(name.substring(0, name.length() - 4) + ".mp3");
            }
        }
        return list;
    }

    /** サウンド識別子が MP3 かどうかを判定する。 */
    public static boolean isMp3Sound(String soundName) {
        return soundName != null && soundName.toLowerCase(Locale.ROOT).endsWith(".mp3");
    }

    /**
     * MP3 識別子からファイルオブジェクトを返す。
     * soundName は "exile_overlay:name.mp3" または "name.mp3" 形式。
     * ディレクトリをスキャンして名前完全一致で探す。
     * new File(dir, name).exists() は Unicode 正規化差異や特殊文字で失敗する場合があるため。
     */
    public static File getMp3File(String soundName) {
        if (soundDir == null) {
            init();
        }
        if (soundName == null || soundDir == null) {
            LOGGER.warn("getMp3File: 引数不正 soundName={}, soundDir={}", soundName, soundDir);
            return null;
        }
        String targetName = soundName.startsWith("exile_overlay:") ? soundName.substring(14) : soundName;

        // OS 由来の File オブジェクトで返すため、パス文字コード問題を回避する
        if (soundDir.isDirectory()) {
            File[] files = soundDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().equals(targetName)) {
                        return f;
                    }
                }
            }
        }
        LOGGER.warn("getMp3File: ファイルが見つかりません name='{}', soundDir='{}'", targetName, soundDir.getAbsolutePath());
        return null;
    }

    /**
     * Converts a display name to a safe ResourceLocation path.
     */
    public static net.minecraft.resources.ResourceLocation getSafeSoundLocation(String name) {
        if (name == null || name.isEmpty()) return null;
        if (!name.startsWith("exile_overlay:")) return new net.minecraft.resources.ResourceLocation(name);
        
        // Custom sound
        String fileName = name.substring("exile_overlay:".length());
        String safeId = java.util.UUID.nameUUIDFromBytes(fileName.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        return new net.minecraft.resources.ResourceLocation("exile_overlay", safeId);
    }
}
