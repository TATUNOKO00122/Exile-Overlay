package com.example.exile_overlay.client.config.position;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HUD位置設定の管理クラス
 * 
 * 【責任】
 * - 各HUD要素の位置設定を一元管理
 * - JSONファイルへの保存・読み込み
 * - デフォルト値の提供
 * 
 * 【シングルトンパターン】
 * 設定はアプリケーション全体で共有される
 */
public class HudPositionManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HudPositionManager.class);
    private static final String CONFIG_FILE_NAME = "hud_positions.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final HudPositionManager INSTANCE = new HudPositionManager();
    
    private final Map<String, HudPosition> positions;
    private final Map<String, HudPosition> defaults;
    private final Map<String, PositionListener> listeners;
    private boolean initialized = false;
    
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
    
    /**
     * 初期化
     * デフォルト値の登録とファイルからの読み込みを行う
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        LOGGER.info("Initializing HudPositionManager...");
        
        registerDefaults();
        loadFromFile();
        
        initialized = true;
        LOGGER.info("HudPositionManager initialized with {} positions", positions.size());
    }
    
    /**
     * デフォルト位置を登録
     */
    private void registerDefaults() {
        // ホットバー: 画面下部中央
        // Orb類はホットバーに連動して表示されるため、独立した位置設定は不要
        defaults.put("hotbar", new HudPosition(Anchor.BOTTOM_CENTER, -1, 0));

        // ダメージポップアップ: 中央付近
        defaults.put("damage_popup", new HudPosition(Anchor.CENTER, 0, -50));

        // バフオーバーレイ: 左上
        defaults.put("buff_overlay", new HudPosition(Anchor.TOP_LEFT, 0, 0, 0.6f, true, false));

        // バニラ酸素ゲージ: ホットバーの上、中央寄せ
        defaults.put("vanilla_air", new HudPosition(Anchor.BOTTOM_CENTER, 12, -39, 0.7f, true, false));

        // バニラ食料ゲージ: ホットバーの上、中央寄せ
        defaults.put("vanilla_food", new HudPosition(Anchor.BOTTOM_CENTER, 12, -33, 0.7f, true, false));

        // スキルホットバー: 画面中央やや下
        defaults.put("skill_hotbar", new HudPosition(Anchor.CENTER, -56, 46, 0.7f, true, true));

        // ターゲットMOB名: 画面上部中央
        defaults.put("target_mob_name", new HudPosition(Anchor.TOP_CENTER, -1, 44, 0.5f, true, false));

        // 装備耐久: 画面下部中央、左寄せ
        defaults.put("armor_durability", new HudPosition(Anchor.BOTTOM_CENTER, -141, 0, 0.5f, true, false));

        LOGGER.debug("Registered {} default positions", defaults.size());
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
            
            JsonObject root = new JsonObject();
            JsonObject positionsJson = new JsonObject();
            
            for (Map.Entry<String, HudPosition> entry : positions.entrySet()) {
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
            root.addProperty("version", 3);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(root, writer);
            }
            
            LOGGER.info("Saved {} positions to {}", positions.size(), configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save positions to file: {}", e.getMessage());
        }
    }
    
    /**
     * ファイルから読み込み
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
            
            JsonObject rootObj = root.getAsJsonObject();
            JsonElement positionsElement = rootObj.get("positions");
            
            if (positionsElement == null || !positionsElement.isJsonObject()) {
                LOGGER.warn("No positions found in config file");
                return;
            }
            
            JsonObject positionsJson = positionsElement.getAsJsonObject();
            int loadedCount = 0;
            
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
                    positions.put(key, position);
                    loadedCount++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse position for {}: {}", key, e.getMessage());
                }
            }
            
            LOGGER.info("Loaded {} positions from file", loadedCount);
        } catch (IOException e) {
            LOGGER.error("Failed to load positions from file: {}", e.getMessage());
        }
    }
    
    /**
     * 設定ファイルのパスを取得
     */
    private File getConfigFile() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir.resolve("config").resolve("exile_overlay").resolve(CONFIG_FILE_NAME).toFile();
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
     * 位置変更リスナーインターフェース
     */
    @FunctionalInterface
    public interface PositionListener {
        void onPositionChanged(String key, HudPosition newPosition);
    }
}
