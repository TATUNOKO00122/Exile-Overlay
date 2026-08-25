package com.example.exile_overlay.client.config.screen.entry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 浮動小数点数（Float）スライダー設定エントリ。
 */
public class FloatSliderConfigEntry extends ConfigEntry {

    private final String translationKey;
    private final float min;
    private final float max;
    private final String format;
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;
    private final Component tooltip;
    private final SliderWidget slider;

    public FloatSliderConfigEntry(String translationKey, Supplier<Float> getter, Consumer<Float> setter,
                                  float min, float max) {
        this(translationKey, getter, setter, min, max, "%.2f", Component.translatable(translationKey + ".tooltip"));
    }

    public FloatSliderConfigEntry(String translationKey, Supplier<Float> getter, Consumer<Float> setter,
                                  float min, float max, String format) {
        this(translationKey, getter, setter, min, max, format, Component.translatable(translationKey + ".tooltip"));
    }

    public FloatSliderConfigEntry(String translationKey, Supplier<Float> getter, Consumer<Float> setter,
                                  float min, float max, String format, Component tooltip) {
        this.translationKey = translationKey;
        this.min = min;
        this.max = max;
        this.format = format;
        this.getter = getter;
        this.setter = setter;
        this.tooltip = tooltip;

        float current = getter.get();
        double initialVal = (max > min) ? (current - min) / (max - min) : 0.0;
        this.slider = new SliderWidget(0, 0, 200, 20, Math.max(0.0, Math.min(1.0, initialVal)));
        if (tooltip != null) {
            this.slider.setTooltip(Tooltip.create(tooltip));
        }
    }

    public void updateState() {
        float current = getter.get();
        double val = (max > min) ? (current - min) / (max - min) : 0.0;
        this.slider.setValueDirect(Math.max(0.0, Math.min(1.0, val)));
    }

    @Override
    public void updateBounds(int x, int y, int width, int height) {
        int btnW = Math.min(260, width - 16);
        int btnX = x + (width - btnW) / 2;
        int btnY = y + (height - 20) / 2;

        slider.setX(btnX);
        slider.setY(btnY);
        slider.setWidth(btnW);
        slider.setHeight(20);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height,
                       int mouseX, int mouseY, boolean isHovered, float partialTick) {
        updateBounds(x, y, width, height);
        slider.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.slider.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.slider.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean matchesSearch(String query) {
        return textMatches(Component.translatable(translationKey), query)
                || (tooltip != null && textMatches(tooltip, query))
                || textMatches(translationKey, query);
    }

    private static float roundByFormat(float val, String format) {
        if (format == null) {
            return Math.round(val * 100.0f) / 100.0f;
        }
        try {
            return Float.parseFloat(String.format(java.util.Locale.ROOT, format, val));
        } catch (Exception e) {
            return Math.round(val * 100.0f) / 100.0f;
        }
    }

    private class SliderWidget extends AbstractSliderButton {
        SliderWidget(int x, int y, int width, int height, double value) {
            super(x, y, width, height, Component.empty(), value);
            this.updateMessage();
        }

        void setValueDirect(double newValue) {
            this.value = newValue;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (format == null) {
                this.setMessage(Component.translatable(translationKey));
            } else {
                float val = min + (max - min) * (float) this.value;
                this.setMessage(Component.translatable(translationKey, String.format(format, val)));
            }
        }

        @Override
        protected void applyValue() {
            float rawValue = min + (max - min) * (float) this.value;
            float newValue = roundByFormat(rawValue, format);
            setter.accept(newValue);
        }
    }
}
