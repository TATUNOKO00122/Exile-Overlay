package com.example.exile_overlay.client.config.position;

import com.example.exile_overlay.client.config.IConfigSection;

import net.minecraft.client.Minecraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HUD位置設定の管理クラス
 * 
 * - 各HUD要素の位置設定を一元管理
 * - JSONファイルへの保存・読み込み
 * - デフォルト値の外部設定ファイル（hud_default_positions.json）対応
 */
public class HudPositionManager implements IConfigSection {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HudPositionManager.class);
    private static final String CONFIG_FILE_NAME = "hud_positions.json";
    private static final String DEFAULTS_CONFIG_FILE_NAME = "hud_default_positions.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final HudPositionManager INSTANCE = new HudPositionManager();
    
    private final Map<String, HudPosition> positions;
    private final Map<String, HudPosition> defaults;
    private final Map<String, PositionListener> listeners;
    private volatile boolean initialized = false;
    private final Object initLock = new Object();
    
    private HudPositionManager() {
        this.positions = new ConcurrentHashMap<>();
        this.defaults = new HashMap<>();
        this.listeners = new ConcurrentHashMap<>();
    }
    
    /**
     * インスタンスを取得
     */
    public static HudPositionManager getInstance() {
        return INSTANCE;
    }

    @Override
    public String getSectionId() {
        return "hud_positions";
    }

    /**
     * IConfigSection準拠のload。デフォルトと位置設定を再読み込み。
     */
    @Override
    public void load() {
        loadDefaultsFromFile();
        loadFromFile();
    }

    /**
     * IConfigSection準拠のsave。既存のsaveToFile()に委譲。
     */
    @Override
    public void save() {
        saveToFile();
    }
    
    /**
     * 初期化
     * デフォルト値の登録・読み込みとファイルからの位置読み込みを行う
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        synchronized (initLock) {
            if (initialized) {
                return;
            }
            LOGGER.info("Initializing HudPositionManager...");
            
            registerDefaults();
            loadDefaultsFromFile();
            loadFromFile();
            
            initialized = true;
            LOGGER.info("HudPositionManager initialized with {} positions ({} defaults)", positions.size(), defaults.size());
        }
    }
    
    /**
     * 内蔵のデフォルト位置を登録（フォールバック用）
     */
    private void registerDefaults() {
        defaults.put("hotbar", new HudPosition(Anchor.BOTTOM_CENTER, 12, 0, 1.3f, true, false));

        defaults.put("damage_popup", new HudPosition(Anchor.CENTER, 0, -50));

        defaults.put("buff_overlay", new HudPosition(Anchor.TOP_LEFT, 0, 0, 0.6f, true, false));

        defaults.put("vanilla_air", new HudPosition(Anchor.BOTTOM_CENTER, 45, -44, 0.8f, true, false));

        defaults.put("vanilla_food", new HudPosition(Anchor.BOTTOM_CENTER, 45, -53, 0.8f, true, false));

        defaults.put("skill_hotbar", new HudPosition(Anchor.CENTER, -90, 72, 0.6f, true, true));

        defaults.put("target_mob_name", new HudPosition(Anchor.TOP_CENTER, 1, 84, 0.7f, true, false));

        defaults.put("armor_durability", new HudPosition(Anchor.BOTTOM_LEFT, 87, 0, 0.7f, true, false));

        defaults.put("boss_hp_bar", new HudPosition(Anchor.TOP_CENTER, 0, 38, 0.6f, true, false));

        defaults.put("gateway_boss_bar", new HudPosition(Anchor.TOP_CENTER, -2, 46, 0.7f, true, false));

        defaults.put("day_counter", new HudPosition(Anchor.BOTTOM_CENTER, 0, 0));

        defaults.put("skill_buff_overlay", new HudPosition(Anchor.TOP_LEFT, 0, 50, 0.6f, false, false));

        defaults.put("damage_tracker", new HudPosition(Anchor.TOP_CENTER, 117, 46, 0.7f, true, false));

        defaults.put("botania_mana_bar", new HudPosition(Anchor.BOTTOM_CENTER, 0, -29, 1.0f, false, false));

        defaults.put("minion_overlay", new HudPosition(Anchor.TOP_LEFT, 0, 50, 0.6f, true, false));

        defaults.put("lightmans_currency_coins", new HudPosition(Anchor.BOTTOM_LEFT, 5, -25, 1.0f, true, false));

        defaults.put("exp_accumulator", new HudPosition(Anchor.TOP_CENTER, 0, 80, 1.0f, false, false));

        defaults.put("dungeon_timer", new HudPosition(Anchor.CENTER, 139, 83, 1.5f, false, false));

        // defaults.put("kill_counter", new HudPosition(Anchor.TOP_LEFT, 5, 30, 1.0f, true, false));

        LOGGER.debug("Registered {} builtin default positions", defaults.size());
    }
    
    /**
     * HUD要素の位置を取得
     * 
     * @param key HUD要素のキー
     * @return 位置設定（未設定の場合はデフォルト値）
     */
    public HudPosition getPosition(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return positions.getOrDefault(key, defaults.getOrDefault(key, new HudPosition()));
    }
    
    /**
     * HUD要素の位置を設定
     * 
     * @param key HUD要素のキー
     * @param position 新しい位置
     */
    public void setPosition(String key, HudPosition position) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(position, "position cannot be null");
        
        HudPosition oldPosition = positions.put(key, position);
        
        if (!position.equals(oldPosition)) {
            notifyListeners(key, position);
        }
    }
    
    /**
     * 設定をデフォルトに戻す
     * 
     * @param key HUD要素のキー
     */
    public void resetToDefault(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        HudPosition defaultPosition = defaults.get(key);
        if (defaultPosition != null) {
            setPosition(key, defaultPosition);
        }
    }
    
    /**
     * 全ての設定をデフォルトに戻す
     */
    public void resetAllToDefaults() {
        positions.clear();
        positions.putAll(defaults);
        defaults.keySet().forEach(this::notifyListeners);
        LOGGER.info("Reset all positions to defaults");
    }
    
    /**
     * 変更リスナーを登録
     * 
     * @param key HUD要素のキー
     * @param listener 変更通知を受け取るリスナー
     */
    public void addListener(String key, PositionListener listener) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.put(key, listener);
    }
    
    /**
     * 変更リスナーを削除
     */
    public void removeListener(String key) {
        listeners.remove(key);
    }
    
    private void notifyListeners(String key, HudPosition position) {
        PositionListener listener = listeners.get(key);
        if (listener != null) {
            try {
                listener.onPositionChanged(key, position);
            } catch (Exception e) {
                LOGGER.error("Error notifying position change for {}: {}", key, e.getMessage());
            }
        }
    }
    
    private void notifyListeners(String key) {
        notifyListeners(key, getPosition(key));
    }
    
    /**
     * ファイルに保存
     */
    public void saveToFile() {
        try {
            File configFile = getConfigFile();
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            JsonObject root = serializePositions(positions, 3);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(root, writer);
            }
            
            LOGGER.info("Saved {} positions to {}", positions.size(), configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save positions to file: {}", e.getMessage());
        }
    }

    /**
     * デフォルト位置をファイルに保存
     */
    public void saveDefaultsToFile() {
        try {
            File configFile = getDefaultsConfigFile();
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            JsonObject root = serializePositions(defaults, 1);

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(root, writer);
            }

            LOGGER.info("Saved {} default positions to {}", defaults.size(), configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save default positions to file: {}", e.getMessage());
        }
    }
    
    /**
     * ファイルから位置設定を読み込み
     */
    private void loadFromFile() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            LOGGER.info("No position config file found, using defaults");
            return;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                LOGGER.warn("Invalid position config file format");
                return;
            }
            
            parsePositions(root.getAsJsonObject(), positions);
            LOGGER.info("Loaded {} positions from file", positions.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load positions from file: {}", e.getMessage());
        }
    }

    /**
     * ファイルからデフォルト位置設定を読み込み
     */
    private void loadDefaultsFromFile() {
        File configFile = getDefaultsConfigFile();
        if (!configFile.exists()) {
            LOGGER.info("No default positions config file found, creating with builtin defaults");
            saveDefaultsToFile();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                LOGGER.warn("Invalid default position config file format");
                return;
            }

            parsePositions(root.getAsJsonObject(), defaults);
            LOGGER.info("Loaded default positions from {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to load default positions from file: {}", e.getMessage());
        }
    }

    /**
     * JSONオブジェクトからHUD位置マップにデシリアライズする
     */
    private void parsePositions(JsonObject rootObj, Map<String, HudPosition> targetMap) {
        JsonElement positionsElement = rootObj.get("positions");
        if (positionsElement == null || !positionsElement.isJsonObject()) {
            positionsElement = rootObj.get("default_positions");
        }

        if (positionsElement == null || !positionsElement.isJsonObject()) {
            LOGGER.warn("No positions element found in json object");
            return;
        }

        JsonObject positionsJson = positionsElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : positionsJson.entrySet()) {
            String key = entry.getKey();
            JsonElement posElement = entry.getValue();

            if (!posElement.isJsonObject()) {
                continue;
            }

            try {
                JsonObject posJson = posElement.getAsJsonObject();
                String anchorName = posJson.get("anchor").getAsString();
                int offsetX = posJson.get("offsetX").getAsInt();
                int offsetY = posJson.get("offsetY").getAsInt();

                float scale = 1.0f;
                if (posJson.has("scale")) {
                    scale = posJson.get("scale").getAsFloat();
                }

                boolean visible = true;
                if (posJson.has("visible")) {
                    visible = posJson.get("visible").getAsBoolean();
                }

                boolean horizontal = false;
                if (posJson.has("horizontal")) {
                    horizontal = posJson.get("horizontal").getAsBoolean();
                }

                Anchor anchor = Anchor.valueOf(anchorName);
                HudPosition position = new HudPosition(anchor, offsetX, offsetY, scale, visible, horizontal);
                targetMap.put(key, position);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse position for {}: {}", key, e.getMessage());
            }
        }
    }

    /**
     * HUD位置マップをJSONオブジェクトへシリアライズする
     */
    private JsonObject serializePositions(Map<String, HudPosition> sourceMap, int version) {
        JsonObject root = new JsonObject();
        JsonObject positionsJson = new JsonObject();

        for (Map.Entry<String, HudPosition> entry : sourceMap.entrySet()) {
            HudPosition pos = entry.getValue();
            JsonObject posJson = new JsonObject();
            posJson.addProperty("anchor", pos.getAnchor().name());
            posJson.addProperty("offsetX", pos.getOffsetX());
            posJson.addProperty("offsetY", pos.getOffsetY());
            posJson.addProperty("scale", pos.getScale());
            posJson.addProperty("visible", pos.isVisible());
            posJson.addProperty("horizontal", pos.isHorizontal());
            positionsJson.add(entry.getKey(), posJson);
        }

        root.add("positions", positionsJson);
        root.addProperty("version", version);
        return root;
    }
    
    /**
     * 設定ファイルのパスを取得
     */
    private File getConfigFile() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir.resolve("config").resolve("exile_overlay").resolve(CONFIG_FILE_NAME).toFile();
    }

    /**
     * デフォルト設定ファイルのパスを取得
     */
    private File getDefaultsConfigFile() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir.resolve("config").resolve("exile_overlay").resolve(DEFAULTS_CONFIG_FILE_NAME).toFile();
    }
    
    /**
     * 登録されている全てのキーを取得
     */
    public Map<String, HudPosition> getAllPositions() {
        return new HashMap<>(positions);
    }
    
    /**
     * デフォルト位置のキー一覧を取得
     */
    public Map<String, HudPosition> getDefaultPositions() {
        return new HashMap<>(defaults);
    }
    
    /**
     * 指定したキーが存在するかチェック
     */
    public boolean hasPosition(String key) {
        return positions.containsKey(key) || defaults.containsKey(key);
    }

    /**
     * 登録されているすべてのキー（デフォルトおよびカスタム含む）を取得
     */
    public Set<String> getAllKeys() {
        Set<String> keys = new HashSet<>(defaults.keySet());
        keys.addAll(positions.keySet());
        return keys;
    }

    /**
     * すべてのHUD要素を非表示に設定し保存する
     */
    public void hideAllElements() {
        for (String key : getAllKeys()) {
            HudPosition pos = getPosition(key);
            if (pos.isVisible()) {
                setPosition(key, pos.withVisible(false));
            }
        }
        saveToFile();
    }
    
    /**
     * 位置変更リスナーインターフェース
     */
    @FunctionalInterface
    public interface PositionListener {
        void onPositionChanged(String key, HudPosition newPosition);
    }
}
