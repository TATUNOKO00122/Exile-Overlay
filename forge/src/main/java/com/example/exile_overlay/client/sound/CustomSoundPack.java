package com.example.exile_overlay.client.sound;

import com.example.exile_overlay.ExileOverlayMod;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CustomSoundPack implements PackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String packId;
    private final File soundDir;
    private final Map<ResourceLocation, File> soundFiles = new HashMap<>();
    private byte[] soundsJsonData;

    public CustomSoundPack(String packId, File soundDir) {
        this.packId = packId;
        this.soundDir = soundDir;
        reload();
    }

    public void reload() {
        soundFiles.clear();
        JsonObject soundsJson = new JsonObject();
        
        if (soundDir.exists() && soundDir.isDirectory()) {
            File[] files = soundDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".ogg"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().substring(0, file.getName().length() - 4);
                    String safeId = java.util.UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
                    // Generate ResourceLocation for the sound file itself
                    ResourceLocation fileLoc = new ResourceLocation(ExileOverlayMod.MOD_ID, "sounds/" + safeId + ".ogg");
                    soundFiles.put(fileLoc, file);
                    
                    // Add entry to sounds.json
                    JsonObject eventObj = new JsonObject();
                    eventObj.addProperty("category", "ui");
                    JsonObject soundObj = new JsonObject();
                    soundObj.addProperty("name", ExileOverlayMod.MOD_ID + ":" + safeId);
                    com.google.gson.JsonArray soundsArray = new com.google.gson.JsonArray();
                    soundsArray.add(soundObj);
                    eventObj.add("sounds", soundsArray);
                    
                    // The SoundEvent name will be e.g., exile_overlay:e3b0c442...
                    soundsJson.add(safeId, eventObj);
                }
            }
        }
        
        soundsJsonData = soundsJson.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES || !location.getNamespace().equals(ExileOverlayMod.MOD_ID)) {
            return null;
        }

        if (location.getPath().equals("sounds.json")) {
            return () -> new ByteArrayInputStream(soundsJsonData);
        }

        File file = soundFiles.get(location);
        if (file != null && file.exists()) {
            return () -> new FileInputStream(file);
        }

        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput) {
        if (type == PackType.CLIENT_RESOURCES && namespace.equals(ExileOverlayMod.MOD_ID)) {
            if (path.equals("") || path.equals("sounds")) {
                resourceOutput.accept(new ResourceLocation(ExileOverlayMod.MOD_ID, "sounds.json"), () -> new ByteArrayInputStream(soundsJsonData));
                for (Map.Entry<ResourceLocation, File> entry : soundFiles.entrySet()) {
                    if (entry.getValue().exists()) {
                        resourceOutput.accept(entry.getKey(), () -> new FileInputStream(entry.getValue()));
                    }
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Collections.singleton(ExileOverlayMod.MOD_ID) : Collections.emptySet();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if (deserializer.getMetadataSectionName().equals("pack")) {
            JsonObject packObj = new JsonObject();
            packObj.addProperty("pack_format", 15); // Format 15 is 1.20-1.20.1
            packObj.addProperty("description", "Exile Overlay Custom Sounds");
            return deserializer.fromJson(packObj);
        }
        return null;
    }

    @Override
    public String packId() {
        return this.packId;
    }

    @Override
    public void close() {
        // No persistent resources to close
    }
}
