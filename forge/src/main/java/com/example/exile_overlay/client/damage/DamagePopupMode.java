package com.example.exile_overlay.client.damage;

import net.minecraft.network.chat.Component;

/**
 * ダメージポップアップの表示・軌道アニメーションモード。
 */
public enum DamagePopupMode {
    /** 螺旋散布 (既存): MOB頭上に螺旋状に散布し、ノックバック方向へ微速移動しながら上昇 */
    SPIRAL_SPREAD("exile_overlay.config.popup_mode.spiral_spread"),
    /** 垂直上昇 (新規): 水平分散せずMOB直上からまっすぐ垂直上方向へスムーズに浮遊上昇 */
    VERTICAL_FLOAT("exile_overlay.config.popup_mode.vertical_float"),
    /** ポップ放物線 (新規): 上方向にピョンと跳ね上がり、重力減速で弧を描いてフワッと上昇 */
    POP_ARC("exile_overlay.config.popup_mode.pop_arc");

    private final String translationKey;

    DamagePopupMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    public static DamagePopupMode fromName(String name) {
        if (name == null || name.isEmpty()) {
            return SPIRAL_SPREAD;
        }
        for (DamagePopupMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return SPIRAL_SPREAD;
    }

    public DamagePopupMode next() {
        DamagePopupMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
