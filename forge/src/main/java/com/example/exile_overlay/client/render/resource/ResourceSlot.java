package com.example.exile_overlay.client.render.resource;

import com.example.exile_overlay.api.DataType;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 単一スロットのリソース管理
 * 
 * 複数のResourceCandidateを保持し、自動的に有効なリソースを選択する
 * 登録順が優先度となる（先に登録されたものが優先）
 */
public class ResourceSlot {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final DataType currentValueType;
    private final DataType maxValueType;
    private final List<ResourceCandidate> candidates;
    private ResourceCandidate lastValidCandidate;
    
    public ResourceSlot(DataType currentValueType, DataType maxValueType) {
        this.currentValueType = currentValueType;
        this.maxValueType = maxValueType;
        this.candidates = new ArrayList<>();
        this.lastValidCandidate = null;
    }
    
    /**
     * リソース候補を登録
     * 登録順が優先順位となる
     */
    public void register(ResourceCandidate candidate) {
        if (candidate == null) {
            LOGGER.warn("Attempted to register null candidate for {}", currentValueType);
            return;
        }
        
        candidates.add(candidate);
        LOGGER.debug("Registered resource candidate '{}' for slot {}", 
                candidate.getId(), currentValueType);
    }
    
    /**
     * 現在選択されている有効なリソース候補を取得
     * 有効な候補がない場合はnullを返す
     */
    public ResourceCandidate getActiveCandidate(Player player) {
        for (ResourceCandidate candidate : candidates) {
            if (candidate.isValid(player, currentValueType)) {
                if (lastValidCandidate != candidate) {
                    LOGGER.debug("Resource switched to '{}' for slot {}", 
                            candidate.getId(), currentValueType);
                    lastValidCandidate = candidate;
                }
                return candidate;
            }
        }
        
        // 有効な候補がない場合、利用可能なものを探す（最大値 > 0）
        for (ResourceCandidate candidate : candidates) {
            if (candidate.isAvailable(player, currentValueType)) {
                if (lastValidCandidate != candidate) {
                    LOGGER.debug("Resource switched to '{}' (available but empty) for slot {}", 
                            candidate.getId(), currentValueType);
                    lastValidCandidate = candidate;
                }
                return candidate;
            }
        }
        
        if (lastValidCandidate != null) {
            LOGGER.debug("No valid resource for slot {}, clearing last candidate", currentValueType);
            lastValidCandidate = null;
        }
        
        return null;
    }
    
    /**
     * 現在値を取得（アクティブな候補から）
     */
    public float getCurrentValue(Player player) {
        ResourceCandidate candidate = getActiveCandidate(player);
        if (candidate != null) {
            return candidate.getCurrentValue(player, currentValueType);
        }
        return 0.0f;
    }
    
    /**
     * 最大値を取得（アクティブな候補から）
     */
    public float getMaxValue(Player player) {
        ResourceCandidate candidate = getActiveCandidate(player);
        if (candidate != null) {
            return candidate.getMaxValue(player, maxValueType);
        }
        return 0.0f;
    }
    
    /**
     * アクティブな候補の色を取得
     */
    public int getActiveColor(Player player) {
        ResourceCandidate candidate = getActiveCandidate(player);
        if (candidate != null) {
            return candidate.getColor();
        }
        // デフォルト色（グレー）
        return 0x808080;
    }
    
    /**
     * アクティブな候補の表示名を取得
     */
    public String getActiveDisplayName(Player player) {
        ResourceCandidate candidate = getActiveCandidate(player);
        if (candidate != null) {
            return candidate.getDisplayName();
        }
        return "Empty";
    }
    
    /**
     * アクティブな候補のIDを取得
     */
    public String getActiveId(Player player) {
        ResourceCandidate candidate = getActiveCandidate(player);
        if (candidate != null) {
            return candidate.getId();
        }
        return "none";
    }
    
    /**
     * 値を表示すべきかどうか
     */
    public boolean shouldShowValue(Player player) {
        ResourceCandidate candidate = getActiveCandidate(player);
        return candidate != null && candidate.shouldShowValue();
    }
    
    /**
     * 登録済み候補リストを取得（コピー）
     */
    public List<ResourceCandidate> getCandidates() {
        return new ArrayList<>(candidates);
    }
    
    /**
     * 候補数を取得
     */
    public int getCandidateCount() {
        return candidates.size();
    }
    
    /**
     * リソースが切り替わったかどうかをチェック
     */
    public boolean hasSwitched(Player player) {
        ResourceCandidate current = getActiveCandidate(player);
        return current != lastValidCandidate;
    }
    
    @Override
    public String toString() {
        return "ResourceSlot{" +
                "type=" + currentValueType +
                ", candidates=" + candidates.size() +
                '}';
    }
}
