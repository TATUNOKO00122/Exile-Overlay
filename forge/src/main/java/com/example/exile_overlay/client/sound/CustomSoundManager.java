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
     * 専用フォルダ内の.oggファイル一覧を取得する
     */
    public static List<String> getAvailableCustomSounds() {
        List<String> list = new ArrayList<>();
        if (soundDir != null && soundDir.exists() && soundDir.isDirectory()) {
            File[] files = soundDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".ogg"));
            if (files != null) {
                for (File file : files) {
                    list.add(file.getName().substring(0, file.getName().length() - 4));
                }
            }
        }
        return list;
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
