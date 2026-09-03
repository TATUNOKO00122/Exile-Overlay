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

    public static File getSoundDir() {
        if (soundDir == null) {
            init();
        }
        return soundDir;
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
     * サウンド識別子がカスタムサウンド（config/exile_overlay/sounds/ 内のファイル）かどうかを判定する。
     */
    public static boolean isCustomSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) return false;
        return soundName.startsWith("exile_overlay:") || getCustomSoundFile(soundName) != null;
    }

    /**
     * サウンド識別子からカスタムサウンドファイル（.ogg / .mp3）を検索して返す。
     * soundName は "exile_overlay:name", "exile_overlay:name.mp3", "name", "name.ogg", "name.mp3" 等に対応。
     */
    public static File getCustomSoundFile(String soundName) {
        if (soundDir == null) {
            init();
        }
        if (soundName == null || soundDir == null || !soundDir.isDirectory()) {
            return null;
        }

        String rawName = soundName.startsWith("exile_overlay:") ? soundName.substring(14) : soundName;
        File[] files = soundDir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            String fName = f.getName();
            if (fName.equalsIgnoreCase(rawName)) {
                return f;
            }
            // 拡張子なしで指定された場合の .ogg / .mp3 マッチ
            if (fName.equalsIgnoreCase(rawName + ".ogg") || fName.equalsIgnoreCase(rawName + ".mp3")) {
                return f;
            }
        }
        return null;
    }

    /**
     * MP3 識別子からファイルオブジェクトを返す（互換用）。
     */
    public static File getMp3File(String soundName) {
        return getCustomSoundFile(soundName);
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
