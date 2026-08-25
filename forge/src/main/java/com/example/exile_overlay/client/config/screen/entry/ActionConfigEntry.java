package com.example.exile_overlay.client.config.screen.entry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * クリック時に特定のアクション（別画面遷移や処理実行）を行うボタンエントリ。
 */
public class ActionConfigEntry extends ConfigEntry {

    private final Component label;
    private final Component tooltip;
    private final Button button;

    public ActionConfigEntry(String translationKey, Consumer<Button> onClick) {
        this(Component.translatable(translationKey), Component.translatable(translationKey + ".tooltip"), onClick);
    }

    public ActionConfigEntry(Component label, Component tooltip, Consumer<Button> onClick) {
        this.label = label;
        this.tooltip = tooltip;

        this.button = Button.builder(label, onClick::accept)
                .bounds(0, 0, 200, 20)
                .build();

        if (tooltip != null) {
            this.button.setTooltip(Tooltip.create(tooltip));
        }
    }

    public void setMessage(Component newLabel) {
        this.button.setMessage(newLabel);
    }

    public void setTooltip(Component newTooltip) {
        this.button.setTooltip(newTooltip != null ? Tooltip.create(newTooltip) : null);
    }

    @Override
    public void updateBounds(int x, int y, int width, int height) {
        int btnW = Math.min(260, width - 16);
        int btnX = x + (width - btnW) / 2;
        int btnY = y + (height - 20) / 2;

        button.setX(btnX);
        button.setY(btnY);
        button.setWidth(btnW);
        button.setHeight(20);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height,
                       int mouseX, int mouseY, boolean isHovered, float partialTick) {
        updateBounds(x, y, width, height);
        button.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.button.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.button.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean matchesSearch(String query) {
        return textMatches(this.label, query)
                || (tooltip != null && textMatches(tooltip, query));
    }
}
