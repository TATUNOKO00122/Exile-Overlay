package com.example.exile_overlay.client.config.screen.entry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 複数の選択肢を順繰りに切り替える設定エントリ（Enumやリスト向け）。
 */
public class CycleConfigEntry<T> extends ConfigEntry {

    private final List<T> values;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Function<T, Component> formatter;
    private final Component tooltip;
    private final Runnable onChange;
    private final Button button;

    public CycleConfigEntry(List<T> values, Supplier<T> getter, Consumer<T> setter,
                            Function<T, Component> formatter, Component tooltip) {
        this(values, getter, setter, formatter, tooltip, null);
    }

    public CycleConfigEntry(List<T> values, Supplier<T> getter, Consumer<T> setter,
                            Function<T, Component> formatter, Component tooltip, Runnable onChange) {
        this.values = values;
        this.getter = getter;
        this.setter = setter;
        this.formatter = formatter;
        this.tooltip = tooltip;
        this.onChange = onChange;

        this.button = Button.builder(formatter.apply(getter.get()), btn -> {
            T current = getter.get();
            int idx = values.indexOf(current);
            int nextIdx = (idx + 1) % values.size();
            T next = values.get(nextIdx);
            setter.accept(next);
            btn.setMessage(formatter.apply(next));
            if (onChange != null) {
                onChange.run();
            }
        }).bounds(0, 0, 200, 20).build();

        if (tooltip != null) {
            this.button.setTooltip(Tooltip.create(tooltip));
        }
    }

    public void updateState() {
        this.button.setMessage(formatter.apply(getter.get()));
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
        Component currentText = formatter.apply(getter.get());
        return textMatches(currentText, query)
                || (tooltip != null && textMatches(tooltip, query));
    }
}
