package com.example.exile_overlay.dmgtracker.gui;

import com.example.exile_overlay.client.damage.DamagePopupConfig;

import java.util.Locale;

public final class ElementColors {

    private static final int DEFAULT_ELEMENT_COLOR = 0xFF8A2BE2;
    private static final int BLOOD_COLOR = 0xFFCC2222;
    private static final int HOLY_COLOR = 0xFFFFF3B0;

    private ElementColors() {}

    public static int colorFor(String elementName) {
        if (elementName == null) return DEFAULT_ELEMENT_COLOR;
        String e = elementName.toLowerCase(Locale.ROOT);

        DamagePopupConfig config = DamagePopupConfig.getInstance();

        if (e.contains("fire")) return config.getFireDamageColor();
        if (e.contains("water") || e.contains("cold") || e.contains("ice")) return config.getIceDamageColor();
        if (e.contains("lightning") || e.contains("thunder")) return config.getLightningDamageColor();
        if (e.contains("nature")) return config.getNatureDamageColor();
        if (e.contains("poison")) return config.getPoisonDamageColor();
        if (e.contains("chaos") || e.contains("shadow") || e.contains("dark") || e.contains("magic")) return config.getMagicDamageColor();
        if (e.contains("phys")) return config.getPhysicalDamageColor();
        if (e.contains("elemental")) return config.getElementalDamageColor();
        if (e.contains("wither")) return config.getWitherDamageColor();

        // 特殊な色のバニラ・MOD固有フォールバック
        if (e.contains("blood")) return BLOOD_COLOR;
        if (e.contains("holy") || e.contains("radiant")) return HOLY_COLOR;

        return config.getNormalDamageColor();
    }
}
