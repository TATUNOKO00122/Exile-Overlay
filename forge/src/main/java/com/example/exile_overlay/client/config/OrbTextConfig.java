package com.example.exile_overlay.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OrbTextConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/OrbTextConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_orb_text.json";
    private static OrbTextConfig instance;

    private static final float[] SCALE_OPTIONS = {0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};

    private boolean showOrbText = true;
    private boolean compactNumbers = false;
    private boolean energyCompact = true;
    private float textScale = 1.25f;
    private float energyTextScale = 1.6f;
    private boolean splitOrb1 = false;

    private OrbTextConfig() {
    }

    public static float[] getScaleOptions() {
        return SCALE_OPTIONS;
    }

    public static String getScaleLabel(float scale) {
        return ((int)(scale * 100)) + "%";
    }

    public static OrbTextConfig getInstance() {
        if (instance == null) {
            instance = new OrbTextConfig();
            instance.load();
        }
        return instance;
    }

    private Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(CONFIG_FILE_NAME);
    }

    public void load() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try {
            String json = Files.readString(configPath);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);

            if (obj.has("showOrbText")) {
                showOrbText = obj.get("showOrbText").getAsBoolean();
            }
            if (obj.has("compactNumbers")) {
                compactNumbers = obj.get("compactNumbers").getAsBoolean();
            }
            if (obj.has("roundEnergyOrb")) {
                energyCompact = obj.get("roundEnergyOrb").getAsBoolean();
            }
            if (obj.has("energyCompact")) {
                energyCompact = obj.get("energyCompact").getAsBoolean();
            }
            if (obj.has("textScale")) {
                textScale = obj.get("textScale").getAsFloat();
            }
            if (obj.has("energyTextScale")) {
                energyTextScale = obj.get("energyTextScale").getAsFloat();
            }
            if (obj.has("splitOrb1")) {
                splitOrb1 = obj.get("splitOrb1").getAsBoolean();
            }

            LOGGER.info("Loaded orb text config: showOrbText={}, compactNumbers={}, energyCompact={}, textScale={}, energyTextScale={}, splitOrb1={}", showOrbText, compactNumbers, energyCompact, textScale, energyTextScale, splitOrb1);
        } catch (IOException e) {
            LOGGER.error("Failed to load orb text config: {}", e.getMessage());
        }
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory: {}", e.getMessage());
            return;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("showOrbText", showOrbText);
        obj.addProperty("compactNumbers", compactNumbers);
        obj.addProperty("energyCompact", energyCompact);
        obj.addProperty("textScale", textScale);
        obj.addProperty("energyTextScale", energyTextScale);
        obj.addProperty("splitOrb1", splitOrb1);

        try {
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved orb text config");
        } catch (IOException e) {
            LOGGER.error("Failed to save orb text config: {}", e.getMessage());
        }
    }

    public boolean isShowOrbText() {
        return showOrbText;
    }

    public void setShowOrbText(boolean show) {
        this.showOrbText = show;
    }

    public boolean isCompactNumbers() {
        return compactNumbers;
    }

    public void setCompactNumbers(boolean compact) {
        this.compactNumbers = compact;
    }

    public float getTextScale() {
        return textScale;
    }

    public void setTextScale(float scale) {
        this.textScale = scale;
    }

    public float cycleTextScale() {
        for (int i = 0; i < SCALE_OPTIONS.length; i++) {
            if (Float.compare(SCALE_OPTIONS[i], textScale) == 0) {
                textScale = SCALE_OPTIONS[(i + 1) % SCALE_OPTIONS.length];
                return textScale;
            }
        }
        textScale = 1.0f;
        return textScale;
    }

    public float getEnergyTextScale() {
        return energyTextScale;
    }

    public void setEnergyTextScale(float scale) {
        this.energyTextScale = scale;
    }

    public float cycleEnergyTextScale() {
        for (int i = 0; i < SCALE_OPTIONS.length; i++) {
            if (Float.compare(SCALE_OPTIONS[i], energyTextScale) == 0) {
                energyTextScale = SCALE_OPTIONS[(i + 1) % SCALE_OPTIONS.length];
                return energyTextScale;
            }
        }
        energyTextScale = 1.0f;
        return energyTextScale;
    }

    public boolean isEnergyCompact() {
        return energyCompact;
    }

    public void setEnergyCompact(boolean compact) {
        this.energyCompact = compact;
    }

    public boolean isSplitOrb1() {
        return splitOrb1;
    }

    public void setSplitOrb1(boolean split) {
        this.splitOrb1 = split;
    }
}
