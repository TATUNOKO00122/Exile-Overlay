package com.example.exile_overlay.client.config.screen.entry;

import com.example.exile_overlay.client.render.entity.EntityHealthBarConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * カラープリセットをプレビューしながら切り替える設定エントリ。
 */
public class ColorPresetConfigEntry extends ConfigEntry {

    private final EntityHealthBarConfig config;
    private final Button button;

    public ColorPresetConfigEntry(EntityHealthBarConfig config) {
        this.config = config;

        this.button = Button.builder(Component.empty(), btn -> {
            EntityHealthBarConfig.ColorPreset current = EntityHealthBarConfig.ColorPreset.fromHex(config.getHealthBarColor());
            EntityHealthBarConfig.ColorPreset[] presets = EntityHealthBarConfig.ColorPreset.values();
            int nextIndex = (current.ordinal() + 1) % presets.length;
            config.setHealthBarColor(presets[nextIndex].getHex());
        }).tooltip(Tooltip.create(Component.translatable("exile_overlay.config.hp_color.tooltip"))).bounds(0, 0, 200, 20).build();
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

        int btnW = Math.min(260, width - 16);
        int btnX = x + (width - btnW) / 2;
        int btnY = y + (height - 20) / 2;

        button.render(guiGraphics, mouseX, mouseY, partialTick);

        EntityHealthBarConfig.ColorPreset preset = EntityHealthBarConfig.ColorPreset.fromHex(config.getHealthBarColor());
        int color = preset.getColorValue();

        int boxSize = 12;
        int boxX = btnX + (btnW - boxSize) / 2;
        int boxY = btnY + (20 - boxSize) / 2;

        guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF000000);
        guiGraphics.fill(boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, color);
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
        return textMatches("hp_color", query)
                || textMatches(Component.translatable("exile_overlay.config.hp_color.tooltip"), query);
    }
}
