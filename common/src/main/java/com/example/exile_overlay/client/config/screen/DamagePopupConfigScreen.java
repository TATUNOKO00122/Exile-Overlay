package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.client.damage.DamagePopupConfig;
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
 * Damageポップアップの設定画面
 *
 * 【機能】
 * - 表示/非表示の切り替え
 * - スケール、表示時間、フェード設定
 */
public class DamagePopupConfigScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamagePopupConfigScreen.class);

    private static final int BACKGROUND_COLOR = 0xCC000000;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 25;
    private static final int COLUMN_SPACING = 40;

    private final Screen parent;
    private final DamagePopupConfig config;

    // チェックボックス
    private Checkbox showDamageCheckbox;
    private Checkbox showHealingCheckbox;
    private Checkbox showPlayerDamageCheckbox;
    private Checkbox enableShadowCheckbox;
    private Checkbox enableStackingCheckbox;
    private Checkbox useCustomFontCheckbox;
    private Checkbox enableDistanceScalingCheckbox;

    // スライダー
    private SliderButton baseScaleSlider;
    private SliderButton criticalScaleSlider;
    private SliderButton displayDurationSlider;
    private SliderButton fadeInDurationSlider;
    private SliderButton fadeOutDurationSlider;
    private SliderButton maxDamageTextsSlider;

    public DamagePopupConfigScreen(Screen parent) {
        super(Component.translatable("screen.exile_overlay.damage_popup_config.title"));
        this.parent = parent;
        this.config = DamagePopupConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        // 画面サイズに応じてカラム位置を計算
        int centerX = this.width / 2;
        int leftColumnX = centerX - BUTTON_WIDTH - COLUMN_SPACING / 2;
        int rightColumnX = centerX + COLUMN_SPACING / 2;
        int startY = 50;

        // === 左カラム：表示設定 ===
        int leftY = startY;

        showDamageCheckbox = new Checkbox(leftColumnX, leftY, 150, BUTTON_HEIGHT,
                Component.translatable("checkbox.exile_overlay.show_damage"), config.isShowDamage());
        addRenderableWidget(showDamageCheckbox);
        leftY += SPACING;

        showHealingCheckbox = new Checkbox(leftColumnX, leftY, 150, BUTTON_HEIGHT,
                Component.translatable("checkbox.exile_overlay.show_healing"), config.isShowHealing());
        addRenderableWidget(showHealingCheckbox);
        leftY += SPACING;

        showPlayerDamageCheckbox = new Checkbox(leftColumnX, leftY, 150, BUTTON_HEIGHT,
                Component.translatable("checkbox.exile_overlay.show_player_damage"), config.isShowPlayerDamage());
        addRenderableWidget(showPlayerDamageCheckbox);
        leftY += SPACING;

        enableShadowCheckbox = new Checkbox(leftColumnX, leftY, 150, BUTTON_HEIGHT,
                Component.translatable("checkbox.exile_overlay.enable_shadow"), config.isEnableShadow());
        addRenderableWidget(enableShadowCheckbox);
        leftY += SPACING;

        enableStackingCheckbox = new Checkbox(leftColumnX, leftY, 150, BUTTON_HEIGHT,
                Component.translatable("checkbox.exile_overlay.enable_stacking"), config.isEnableDamageStacking());
        addRenderableWidget(enableStackingCheckbox);
        leftY += SPACING;

        useCustomFontCheckbox = new Checkbox(leftColumnX, leftY, 150, BUTTON_HEIGHT,
                Component.translatable("checkbox.exile_overlay.use_custom_font"), config.isUseCustomFont());
        addRenderableWidget(useCustomFontCheckbox);
        leftY += SPACING;

        enableDistanceScalingCheckbox = new Checkbox(leftColumnX, leftY, 150, BUTTON_HEIGHT,
                Component.translatable("checkbox.exile_overlay.enable_distance_scaling"), config.isEnableDistanceScaling());
        addRenderableWidget(enableDistanceScalingCheckbox);

        // === 右カラム：数値設定 ===
        int rightY = startY;

        baseScaleSlider = new SliderButton(rightColumnX, rightY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("slider.exile_overlay.base_scale"),
                config.getBaseScale(), 0.01, 0.1, 0.001,
                value -> Component.literal(String.format("%.3f", value)));
        addRenderableWidget(baseScaleSlider);
        rightY += SPACING;

        criticalScaleSlider = new SliderButton(rightColumnX, rightY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("slider.exile_overlay.critical_scale"),
                config.getCriticalScale(), 0.01, 0.15, 0.001,
                value -> Component.literal(String.format("%.3f", value)));
        addRenderableWidget(criticalScaleSlider);
        rightY += SPACING;

        displayDurationSlider = new SliderButton(rightColumnX, rightY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("slider.exile_overlay.display_duration"),
                config.getDisplayDuration(), 20, 200, 1,
                value -> Component.literal(String.valueOf(value.intValue()) + " ticks"));
        addRenderableWidget(displayDurationSlider);
        rightY += SPACING;

        fadeInDurationSlider = new SliderButton(rightColumnX, rightY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("slider.exile_overlay.fade_in"),
                config.getFadeInDuration(), 0, 20, 1,
                value -> Component.literal(String.valueOf(value.intValue()) + " ticks"));
        addRenderableWidget(fadeInDurationSlider);
        rightY += SPACING;

        fadeOutDurationSlider = new SliderButton(rightColumnX, rightY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("slider.exile_overlay.fade_out"),
                config.getFadeOutDuration(), 5, 60, 1,
                value -> Component.literal(String.valueOf(value.intValue()) + " ticks"));
        addRenderableWidget(fadeOutDurationSlider);
        rightY += SPACING;

        maxDamageTextsSlider = new SliderButton(rightColumnX, rightY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("slider.exile_overlay.max_texts"),
                config.getMaxDamageTexts(), 5, 50, 1,
                value -> Component.literal(String.valueOf(value.intValue())));
        addRenderableWidget(maxDamageTextsSlider);

        // === ボタン ===
        int buttonY = this.height - 40;

        Button saveButton = Button.builder(
                        Component.translatable("button.exile_overlay.save"),
                        button -> onSave())
                .bounds(centerX - BUTTON_WIDTH - 10, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        Button cancelButton = Button.builder(
                        Component.translatable("button.exile_overlay.cancel"),
                        button -> onCancel())
                .bounds(centerX + 10, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderScreenBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // セクションラベル（画面サイズに応じて位置を計算）
        int centerX = this.width / 2;
        int leftColumnX = centerX - BUTTON_WIDTH - COLUMN_SPACING / 2;
        int rightColumnX = centerX + COLUMN_SPACING / 2;
        graphics.drawString(this.font, Component.translatable("label.exile_overlay.display_settings"), leftColumnX, 35, 0xAAAAAA);
        graphics.drawString(this.font, Component.translatable("label.exile_overlay.numeric_settings"), rightColumnX, 35, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderScreenBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
    }

    private void onSave() {
        LOGGER.info("Saving damage popup config");

        config.setShowDamage(showDamageCheckbox.selected());
        config.setShowHealing(showHealingCheckbox.selected());
        config.setShowPlayerDamage(showPlayerDamageCheckbox.selected());
        config.setEnableShadow(enableShadowCheckbox.selected());
        config.setBaseScale((float) baseScaleSlider.getValue());
        config.setCriticalScale((float) criticalScaleSlider.getValue());
        config.setDisplayDuration((int) displayDurationSlider.getValue());
        config.setFadeInDuration((int) fadeInDurationSlider.getValue());
        config.setFadeOutDuration((int) fadeOutDurationSlider.getValue());
        config.setMaxDamageTexts((int) maxDamageTextsSlider.getValue());
        config.setEnableDamageStacking(enableStackingCheckbox.selected());
        config.setUseCustomFont(useCustomFontCheckbox.selected());
        config.setEnableDistanceScaling(enableDistanceScalingCheckbox.selected());

        config.save();
        this.minecraft.setScreen(parent);
    }

    private void onCancel() {
        LOGGER.info("Canceling damage popup config changes");
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
            // 値の変更時の処理（リアルタイム更新が必要な場合）
        }

        public double getValue() {
            double rawValue = minValue + this.value * (maxValue - minValue);
            // stepに基づいて適切に丸める
            if (step >= 1.0) {
                return Math.round(rawValue);
            } else {
                // stepの小数点以下の桁数を計算
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
