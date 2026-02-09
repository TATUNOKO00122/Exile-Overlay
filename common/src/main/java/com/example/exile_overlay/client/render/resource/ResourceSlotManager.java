package com.example.exile_overlay.client.render.resource;

import com.example.exile_overlay.api.DataType;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 全スロットのリソース管理
 * 
 * ORB_1, ORB_1_OVERLAY, ORB_2, ORB_3など全てのスロットを一元管理
 * 各スロットは独立してリソース候補を保持し、自動的に有効なリソースを選択する
 */
public class ResourceSlotManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceSlotManager INSTANCE = new ResourceSlotManager();
    
    private final Map<String, ResourceSlot> slots;
    
    private ResourceSlotManager() {
        this.slots = new HashMap<>();
        initializeDefaultSlots();
    }
    
    public static ResourceSlotManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * デフォルトスロットを初期化
     */
    private void initializeDefaultSlots() {
        // ORB_1: HP（バニラ固定、切り替えなし）
        createSlot("orb1", DataType.ORB_1_CURRENT, DataType.ORB_1_MAX);
        
        // ORB_1_OVERLAY: Shield（オーバーレイ用）
        createSlot("orb1_overlay", DataType.ORB_1_OVERLAY_CURRENT, DataType.ORB_1_OVERLAY_MAX);
        
        // ORB_2: メインリソース（マナ、血、エネルギーなど）
        createSlot("orb2", DataType.ORB_2_CURRENT, DataType.ORB_2_MAX);
        
        // ORB_3: サブリソース（スタミナ、エネルギーなど）
        createSlot("orb3", DataType.ORB_3_CURRENT, DataType.ORB_3_MAX);
        
        LOGGER.info("Initialized {} resource slots", slots.size());
    }
    
    /**
     * スロットを作成
     */
    public ResourceSlot createSlot(String slotId, DataType currentType, DataType maxType) {
        ResourceSlot slot = new ResourceSlot(currentType, maxType);
        slots.put(slotId, slot);
        LOGGER.debug("Created resource slot '{}' for {}", slotId, currentType);
        return slot;
    }
    
    /**
     * スロットを取得
     */
    public ResourceSlot getSlot(String slotId) {
        return slots.get(slotId);
    }
    
    /**
     * ORB_1 スロットを取得
     */
    public ResourceSlot getOrb1Slot() {
        return slots.get("orb1");
    }
    
    /**
     * ORB_1_OVERLAY スロットを取得
     */
    public ResourceSlot getOrb1OverlaySlot() {
        return slots.get("orb1_overlay");
    }
    
    /**
     * ORB_2 スロットを取得
     */
    public ResourceSlot getOrb2Slot() {
        return slots.get("orb2");
    }
    
    /**
     * ORB_3 スロットを取得
     */
    public ResourceSlot getOrb3Slot() {
        return slots.get("orb3");
    }
    
    /**
     * 指定スロットにリソース候補を登録
     */
    public void registerCandidate(String slotId, ResourceCandidate candidate) {
        ResourceSlot slot = slots.get(slotId);
        if (slot != null) {
            slot.register(candidate);
        } else {
            LOGGER.warn("Attempted to register candidate for unknown slot: {}", slotId);
        }
    }
    
    /**
     * 全スロットの現在値を取得
     */
    public Map<String, Float> getAllCurrentValues(Player player) {
        Map<String, Float> values = new HashMap<>();
        for (Map.Entry<String, ResourceSlot> entry : slots.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getCurrentValue(player));
        }
        return values;
    }
    
    /**
     * 全スロットの最大値を取得
     */
    public Map<String, Float> getAllMaxValues(Player player) {
        Map<String, Float> values = new HashMap<>();
        for (Map.Entry<String, ResourceSlot> entry : slots.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getMaxValue(player));
        }
        return values;
    }
    
    /**
     * 全スロットのアクティブなリソースIDを取得
     */
    public Map<String, String> getAllActiveIds(Player player) {
        Map<String, String> ids = new HashMap<>();
        for (Map.Entry<String, ResourceSlot> entry : slots.entrySet()) {
            ids.put(entry.getKey(), entry.getValue().getActiveId(player));
        }
        return ids;
    }
    
    /**
     * 特定スロットの現在値を取得（簡易アクセス）
     */
    public float getCurrentValue(String slotId, Player player) {
        ResourceSlot slot = slots.get(slotId);
        return slot != null ? slot.getCurrentValue(player) : 0.0f;
    }
    
    /**
     * 特定スロットの最大値を取得（簡易アクセス）
     */
    public float getMaxValue(String slotId, Player player) {
        ResourceSlot slot = slots.get(slotId);
        return slot != null ? slot.getMaxValue(player) : 0.0f;
    }
    
    /**
     * 特定スロットのアクティブな色を取得（簡易アクセス）
     */
    public int getActiveColor(String slotId, Player player) {
        ResourceSlot slot = slots.get(slotId);
        return slot != null ? slot.getActiveColor(player) : 0x808080;
    }
    
    /**
     * 特定スロットのアクティブな表示名を取得（簡易アクセス）
     */
    public String getActiveDisplayName(String slotId, Player player) {
        ResourceSlot slot = slots.get(slotId);
        return slot != null ? slot.getActiveDisplayName(player) : "Empty";
    }
    
    /**
     * リソース切り替えを強制的に再評価
     */
    public void refreshAll(Player player) {
        for (ResourceSlot slot : slots.values()) {
            slot.getActiveCandidate(player);
        }
    }
    
    /**
     * リソース切り替えが発生したかどうかをチェック
     */
    public boolean hasAnySwitched(Player player) {
        for (ResourceSlot slot : slots.values()) {
            if (slot.hasSwitched(player)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 全スロット情報をデバッグ出力
     */
    public void debugPrint(Player player) {
        LOGGER.debug("=== ResourceSlotManager Debug ===");
        for (Map.Entry<String, ResourceSlot> entry : slots.entrySet()) {
            String slotId = entry.getKey();
            ResourceSlot slot = entry.getValue();
            String activeId = slot.getActiveId(player);
            float current = slot.getCurrentValue(player);
            float max = slot.getMaxValue(player);
            LOGGER.debug("Slot {}: {} ({} / {})", slotId, activeId, current, max);
        }
    }
}
