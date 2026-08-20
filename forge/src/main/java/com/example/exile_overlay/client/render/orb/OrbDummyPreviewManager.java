package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.client.config.OrbColorConfig;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * オーブカラー調整画面用のダミー表示および状態管理クラス。
 * ダミープレビューフラグ管理、プレビュー用値の供給、MOD環境に応じたカラータイプリストの提供。
 */
public class OrbDummyPreviewManager {

    private static final OrbDummyPreviewManager INSTANCE = new OrbDummyPreviewManager();

    private boolean dummyPreviewActive = false;

    private OrbDummyPreviewManager() {}

    public static OrbDummyPreviewManager getInstance() {
        return INSTANCE;
    }

    public boolean isDummyPreviewActive() {
        return dummyPreviewActive;
    }

    public void setDummyPreviewActive(boolean active) {
        this.dummyPreviewActive = active;
    }

    /**
     * オーブカラーの定義 Enum
     */
    public enum OrbTarget {
        HEALTH("screen.exile_overlay.orb_color.health", "Health (HP)", OrbColorConfig::getHealthColor, OrbColorConfig::setHealthColor),
        SHIELD("screen.exile_overlay.orb_color.shield", "Shield (ES)", OrbColorConfig::getShieldColor, OrbColorConfig::setShieldColor),
        MANA("screen.exile_overlay.orb_color.mana", "Mana", OrbColorConfig::getManaColor, OrbColorConfig::setManaColor),
        BLOOD("screen.exile_overlay.orb_color.blood", "Blood Magic", OrbColorConfig::getBloodColor, OrbColorConfig::setBloodColor),
        ENERGY("screen.exile_overlay.orb_color.energy", "Energy", OrbColorConfig::getEnergyColor, OrbColorConfig::setEnergyColor),
        FOOD("screen.exile_overlay.orb_color.food", "Food", OrbColorConfig::getFoodColor, OrbColorConfig::setFoodColor);

        private final String translationKey;
        private final String fallbackName;
        private final Function<OrbColorConfig, Integer> getter;
        private final BiConsumer<OrbColorConfig, Integer> setter;

        OrbTarget(String translationKey, String fallbackName, Function<OrbColorConfig, Integer> getter, BiConsumer<OrbColorConfig, Integer> setter) {
            this.translationKey = translationKey;
            this.fallbackName = fallbackName;
            this.getter = getter;
            this.setter = setter;
        }

        public Component getDisplayName() {
            return Component.translatableWithFallback(translationKey, fallbackName);
        }

        public String getFallbackName() {
            return fallbackName;
        }

        public int getColor(OrbColorConfig config) {
            return getter.apply(config);
        }

        public void setColor(OrbColorConfig config, int color) {
            setter.accept(config, color);
        }
    }

    /**
     * 現在のMOD環境および構成から、アクティブに使用されるオーブターゲットの一覧を取得
     */
    public List<OrbTarget> getActiveTargets() {
        List<OrbTarget> targets = new ArrayList<>();
        boolean isMsLoaded = ModList.get().isLoaded("mmorpg");

        // HP, Shield, Mana は常に表示
        targets.add(OrbTarget.HEALTH);
        targets.add(OrbTarget.SHIELD);
        targets.add(OrbTarget.MANA);

        if (isMsLoaded) {
            // Mine and Slash環境では Blood Magic と Energy が有効
            targets.add(OrbTarget.BLOOD);
            targets.add(OrbTarget.ENERGY);
            // MS環境では Food オーブは使用されない
        } else {
            // バニラ/非MS環境では Food が有効
            targets.add(OrbTarget.FOOD);
        }

        return targets;
    }
}
