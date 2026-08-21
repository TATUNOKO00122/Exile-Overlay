package com.example.exile_overlay.client.config.screen.entry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 設定画面のセクション見出しエントリ。
 */
public class SectionHeaderEntry extends ConfigEntry {

    private final Component label;

    public SectionHeaderEntry(String titleKey) {
        this(Component.translatable(titleKey));
    }

    public SectionHeaderEntry(Component label) {
        this.label = label;
    }

    @Override
    public int getHeight() {
        return 28;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height,
                       int mouseX, int mouseY, boolean isHovered, float partialTick) {
        int textY = y + 8;
        guiGraphics.drawString(this.font, this.label, x + 4, textY, 0xFFFFAA);
        guiGraphics.fill(x + 4, textY + 12, x + width - 8, textY + 13, 0x33FFFFFF);
    }

    @Override
    public boolean matchesSearch(String query) {
        return textMatches(this.label, query);
    }
}
