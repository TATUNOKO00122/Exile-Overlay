package com.example.exile_overlay.client.config.screen.entry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 整数（Int）スライダー設定エントリ。
 */
public class IntSliderConfigEntry extends ConfigEntry {

    private final String translationKey;
    private final int min;
    private final int max;
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;
    private final Function<Integer, Component> customFormatter;
    private final Component tooltip;
    private final SliderWidget slider;

    public IntSliderConfigEntry(String translationKey, Supplier<Integer> getter, Consumer<Integer> setter,
                                int min, int max) {
        this(translationKey, getter, setter, min, max, null, Component.translatable(translationKey + ".tooltip"));
    }

    public IntSliderConfigEntry(String translationKey, Supplier<Integer> getter, Consumer<Integer> setter,
                                int min, int max, Function<Integer, Component> customFormatter) {
        this(translationKey, getter, setter, min, max, customFormatter, Component.translatable(translationKey + ".tooltip"));
    }

    public IntSliderConfigEntry(String translationKey, Supplier<Integer> getter, Consumer<Integer> setter,
                                int min, int max, Function<Integer, Component> customFormatter, Component tooltip) {
        this.translationKey = translationKey;
        this.min = min;
        this.max = max;
        this.getter = getter;
        this.setter = setter;
        this.customFormatter = customFormatter;
        this.tooltip = tooltip;

        int current = getter.get();
        double initialVal = (max > min) ? (double) (current - min) / (max - min) : 0.0;
        this.slider = new SliderWidget(0, 0, 200, 20, Math.max(0.0, Math.min(1.0, initialVal)));
        if (tooltip != null) {
            this.slider.setTooltip(Tooltip.create(tooltip));
        }
    }

    public void updateState() {
        int current = getter.get();
        double val = (max > min) ? (double) (current - min) / (max - min) : 0.0;
        this.slider.setValueDirect(Math.max(0.0, Math.min(1.0, val)));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height,
                       int mouseX, int mouseY, boolean isHovered, float partialTick) {
        int btnW = Math.min(260, width - 16);
        int btnX = x + (width - btnW) / 2;
        int btnY = y + (height - 20) / 2;

        slider.setX(btnX);
        slider.setY(btnY);
        slider.setWidth(btnW);
        slider.setHeight(20);

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
            int val = min + (int) Math.round((max - min) * this.value);
            if (customFormatter != null) {
                this.setMessage(customFormatter.apply(val));
            } else {
                this.setMessage(Component.translatable(translationKey, val));
            }
        }

        @Override
        protected void applyValue() {
            int newValue = min + (int) Math.round((max - min) * this.value);
            setter.accept(newValue);
        }
    }
}
