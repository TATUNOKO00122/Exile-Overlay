package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全般設定画面
 * 
 * 【機能】
 * - アイコンサイズ設定（12-24、ステップ2）
 * - パーセント表示/実数値表示切り替え
 */
public class GeneralConfigScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralConfigScreen.class);
    private static final int BACKGROUND_COLOR = 0xCC000000;
    private static final String DAY_COUNTER_KEY = "day_counter";

    private final Screen parent;
    private final EquipmentDisplayConfig config;

    private Checkbox usePercentageCheckbox;
    private Checkbox enableShadowCheckbox;
    private Checkbox autoQuickLootCheckbox;
    private Checkbox dayCounterEnabledCheckbox;
    private SliderButton dayCounterScaleSlider;

    public GeneralConfigScreen(Screen parent) {
        super(Component.translatable("screen.exile_overlay.general_config.title"));
        this.parent = parent;
        this.config = EquipmentDisplayConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int columnWidth = 200;
        int buttonHeight = 20;
        int elementSpacing = 28;

        int titleHeight = 20;
        int labelHeight = 20;
        int footerHeight = 50;
        int sectionHeaderHeight = 25;
        int itemCount = 5;
        int dayCounterItems = 2;
        int contentHeight = titleHeight + labelHeight + (itemCount * elementSpacing) + sectionHeaderHeight + (dayCounterItems * elementSpacing) + footerHeight;

        int startY = Math.max(10, (this.height - contentHeight) / 2);
        int widgetStartY = startY + titleHeight + labelHeight;

        int widgetX = centerX - columnWidth / 2;
        int currentY = widgetStartY;

        // パーセント表示チェックボックス
        usePercentageCheckbox = new Checkbox(widgetX, currentY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.use_percentage"),
                config.isUsePercentage());
        addRenderableWidget(usePercentageCheckbox);
        currentY += elementSpacing;

        // 影表示チェックボックス
        enableShadowCheckbox = new Checkbox(widgetX, currentY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.enable_shadow"),
                config.isEnableShadow());
        addRenderableWidget(enableShadowCheckbox);
        currentY += elementSpacing;

        // Auto Quick Lootチェックボックス
        autoQuickLootCheckbox = new Checkbox(widgetX, currentY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.auto_quick_loot"),
                config.isAutoQuickLootEnabled());
        addRenderableWidget(autoQuickLootCheckbox);
        currentY += elementSpacing + 10;

        // セクション区切り用のスペース（render()で描画）
        currentY += 10;

        // 日数カウンター有効チェックボックス
        HudPosition dayCounterPos = HudPositionManager.getInstance().getPosition(DAY_COUNTER_KEY);
        dayCounterEnabledCheckbox = new Checkbox(widgetX, currentY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.day_counter_enabled"),
                dayCounterPos.isVisible());
        addRenderableWidget(dayCounterEnabledCheckbox);
        currentY += elementSpacing;

        // 日数カウンター scaleスライダー
        float currentScale = dayCounterPos.getScale();
        dayCounterScaleSlider = new SliderButton(widgetX, currentY, columnWidth, buttonHeight,
                Component.translatable("slider.exile_overlay.day_counter_scale"),
                currentScale, 0.5f, 3.0f, 0.1f,
                value -> Component.literal(String.format("%.1f", value)));
        dayCounterScaleSlider.active = dayCounterPos.isVisible();
        addRenderableWidget(dayCounterScaleSlider);

        // ボタン行
        int buttonY = currentY + elementSpacing + 20;
        int actionButtonWidth = 150;

        Button saveButton = Button.builder(
                Component.translatable("button.exile_overlay.save"),
                button -> onSave())
                .bounds(centerX - actionButtonWidth - 10, buttonY, actionButtonWidth, buttonHeight)
                .build();

        Button cancelButton = Button.builder(
                Component.translatable("button.exile_overlay.cancel"),
                button -> onCancel())
                .bounds(centerX + 10, buttonY, actionButtonWidth, buttonHeight)
                .build();

        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderScreenBackground(graphics);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // 日数カウンターセクション区切り
        int centerX = this.width / 2;
        int sectionY = autoQuickLootCheckbox.getY() + 38;
        String sectionTitle = "─── " + Component.translatable("section.exile_overlay.day_counter").getString() + " ───";
        graphics.drawCenteredString(this.font, sectionTitle, centerX, sectionY, 0xAAAAAA);

        // チェックボックス状態に応じてスライダーの有効/無効を更新
        dayCounterScaleSlider.active = dayCounterEnabledCheckbox.selected();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderScreenBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
    }

    private void onSave() {
        LOGGER.info("Saving equipment display config");

        config.setUsePercentage(usePercentageCheckbox.selected());
        config.setEnableShadow(enableShadowCheckbox.selected());
        config.setAutoQuickLootEnabled(autoQuickLootCheckbox.selected());
        config.save();

        // 日数カウンター設定を保存
        float dayCounterScale = (float) dayCounterScaleSlider.getValue();
        boolean dayCounterEnabled = dayCounterEnabledCheckbox.selected();
        HudPosition dayCounterPos = HudPositionManager.getInstance().getPosition(DAY_COUNTER_KEY);
        HudPositionManager.getInstance().setPosition(DAY_COUNTER_KEY, 
            dayCounterPos.withScale(dayCounterScale).withVisible(dayCounterEnabled));
        HudPositionManager.getInstance().saveToFile();
        LOGGER.info("Saved day counter: enabled={}, scale={}", dayCounterEnabled, dayCounterScale);

        this.minecraft.setScreen(parent);
    }

    private void onCancel() {
        LOGGER.info("Canceling equipment display config changes");
        this.minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * カスタムスライダーボタン
     */
    private static class SliderButton extends AbstractSliderButton {
        private final double minValue;
        private final double maxValue;
        private final double step;
        private final Component title;
        private final java.util.function.Function<Double, Component> valueFormatter;

        public SliderButton(int x, int y, int width, int height, Component title,
                double currentValue, double minValue, double maxValue, double step,
                java.util.function.Function<Double, Component> valueFormatter) {
            super(x, y, width, height, title, (currentValue - minValue) / (maxValue - minValue));
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.step = step;
            this.title = title;
            this.valueFormatter = valueFormatter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            Component valueComponent = valueFormatter.apply(getValue());
            this.setMessage(Component.literal("").append(title).append(": ").append(valueComponent));
        }

        @Override
        protected void applyValue() {
            // 値の変更時の処理
        }

        public double getValue() {
            double rawValue = minValue + this.value * (maxValue - minValue);
            if (step >= 1.0) {
                return Math.round(rawValue);
            } else {
                int decimals = 0;
                double tempStep = step;
                while (tempStep < 1.0 && decimals < 10) {
                    tempStep *= 10;
                    decimals++;
                }
                double multiplier = Math.pow(10, decimals);
                return Math.round(rawValue * multiplier) / multiplier;
            }
        }
    }
}
