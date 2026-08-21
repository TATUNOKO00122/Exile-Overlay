package com.example.exile_overlay.client.config.screen.entry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ON/OFFを切り替えるブーリアン設定エントリ。
 */
public class BooleanConfigEntry extends ConfigEntry {

    private final String translationKey;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private final Component tooltip;
    private final Button button;

    private final java.util.function.Function<Boolean, Component> customTextFormatter;
    private final Runnable onChange;

    public BooleanConfigEntry(String translationKey, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this(translationKey, getter, setter, Component.translatable(translationKey + ".tooltip"));
    }

    public BooleanConfigEntry(String translationKey, Supplier<Boolean> getter, Consumer<Boolean> setter, Component tooltip) {
        this(translationKey, getter, setter, null, tooltip, null);
    }

    public BooleanConfigEntry(String translationKey, Supplier<Boolean> getter, Consumer<Boolean> setter,
                              java.util.function.Function<Boolean, Component> customTextFormatter,
                              Component tooltip, Runnable onChange) {
        this.translationKey = translationKey;
        this.getter = getter;
        this.setter = setter;
        this.customTextFormatter = customTextFormatter;
        this.tooltip = tooltip;
        this.onChange = onChange;

        this.button = Button.builder(getButtonText(), btn -> {
            boolean next = !getter.get();
            setter.accept(next);
            btn.setMessage(getButtonText());
            if (onChange != null) {
                onChange.run();
            }
        }).bounds(0, 0, 200, 20).build();

        if (tooltip != null) {
            this.button.setTooltip(Tooltip.create(tooltip));
        }
    }

    private Component getButtonText() {
        boolean enabled = getter.get();
        if (customTextFormatter != null) {
            return customTextFormatter.apply(enabled);
        }
        return Component.translatable(translationKey,
                Component.translatable(enabled ? "exile_overlay.config.on" : "exile_overlay.config.off"));
    }

    public void updateState() {
        this.button.setMessage(getButtonText());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height,
                       int mouseX, int mouseY, boolean isHovered, float partialTick) {
        int btnW = Math.min(260, width - 16);
        int btnX = x + (width - btnW) / 2;
        int btnY = y + (height - 20) / 2;

        button.setX(btnX);
        button.setY(btnY);
        button.setWidth(btnW);
        button.setHeight(20);

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
        return textMatches(Component.translatable(translationKey), query)
                || (tooltip != null && textMatches(tooltip, query))
                || textMatches(translationKey, query);
    }
}
