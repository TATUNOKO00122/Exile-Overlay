package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.client.damage.DamageFontRenderer;
import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.damage.FontPreset;
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

    private final Screen parent;
    private final DamagePopupConfig config;

    // チェックボックス
    private Checkbox showDamageCheckbox;
    private Checkbox showHealingCheckbox;
    private Checkbox showPlayerDamageCheckbox;
    private Checkbox enableShadowCheckbox;
    private Checkbox enableStackingCheckbox;

    // フォント選択
    private FontPreset currentFontPreset;
    private Button fontPresetButton;

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

        int centerX = this.width / 2;
        int columnWidth = 180;
        int buttonHeight = 20;
        int columnSpacing = 40;
        int elementSpacing = 24;

        int leftColumnX = centerX - columnWidth - columnSpacing / 2;
        int rightColumnX = centerX + columnSpacing / 2;

        int itemsPerColumn = 7;
        int titleHeight = 20;
        int labelHeight = 20;
        int footerHeight = 40;
        int contentHeight = titleHeight + labelHeight + (itemsPerColumn * elementSpacing) + footerHeight;

        int startY = Math.max(10, (this.height - contentHeight) / 2);
        int widgetStartY = startY + titleHeight + labelHeight;

        int leftY = widgetStartY;

        showDamageCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.show_damage"), config.isShowDamage());
        addRenderableWidget(showDamageCheckbox);
        leftY += elementSpacing;

        showHealingCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.show_healing"), config.isShowHealing());
        addRenderableWidget(showHealingCheckbox);
        leftY += elementSpacing;

        showPlayerDamageCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.show_player_damage"), config.isShowPlayerDamage());
        addRenderableWidget(showPlayerDamageCheckbox);
        leftY += elementSpacing;

        enableShadowCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.enable_shadow"), config.isEnableShadow());
        addRenderableWidget(enableShadowCheckbox);
        leftY += elementSpacing;

        enableStackingCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, buttonHeight,
                Component.translatable("checkbox.exile_overlay.enable_stacking"), config.isEnableDamageStacking());
        addRenderableWidget(enableStackingCheckbox);
        leftY += elementSpacing;

        currentFontPreset = config.getFontPreset();
        fontPresetButton = Button.builder(
                getFontPresetButtonText(),
                button -> cycleFontPreset())
                .bounds(leftColumnX, leftY, columnWidth, buttonHeight)
                .build();
        addRenderableWidget(fontPresetButton);
        leftY += elementSpacing;

        int rightY = widgetStartY;

        baseScaleSlider = new SliderButton(rightColumnX, rightY, columnWidth, buttonHeight,
                Component.translatable("slider.exile_overlay.base_scale"),
                config.getBaseScale(), 0.01, 0.3, 0.005,
                value -> Component.literal(String.format("%.3f", value)));
        addRenderableWidget(baseScaleSlider);
        rightY += elementSpacing;

        criticalScaleSlider = new SliderButton(rightColumnX, rightY, columnWidth, buttonHeight,
                Component.translatable("slider.exile_overlay.critical_scale"),
                config.getCriticalScale(), 0.01, 0.5, 0.01,
                value -> Component.literal(String.format("%.3f", value)));
        addRenderableWidget(criticalScaleSlider);
        rightY += elementSpacing;

        displayDurationSlider = new SliderButton(rightColumnX, rightY, columnWidth, buttonHeight,
                Component.translatable("slider.exile_overlay.display_duration"),
                config.getDisplayDuration(), 20, 200, 1,
                value -> Component.literal(String.valueOf(value.intValue()) + " ticks"));
        addRenderableWidget(displayDurationSlider);
        rightY += elementSpacing;

        fadeInDurationSlider = new SliderButton(rightColumnX, rightY, columnWidth, buttonHeight,
                Component.translatable("slider.exile_overlay.fade_in"),
                config.getFadeInDuration(), 0, 20, 1,
                value -> Component.literal(String.valueOf(value.intValue()) + " ticks"));
        addRenderableWidget(fadeInDurationSlider);
        rightY += elementSpacing;

        fadeOutDurationSlider = new SliderButton(rightColumnX, rightY, columnWidth, buttonHeight,
                Component.translatable("slider.exile_overlay.fade_out"),
                config.getFadeOutDuration(), 5, 60, 1,
                value -> Component.literal(String.valueOf(value.intValue()) + " ticks"));
        addRenderableWidget(fadeOutDurationSlider);
        rightY += elementSpacing;

        maxDamageTextsSlider = new SliderButton(rightColumnX, rightY, columnWidth, buttonHeight,
                Component.translatable("slider.exile_overlay.max_texts"),
                config.getMaxDamageTexts(), 5, 50, 1,
                value -> Component.literal(String.valueOf(value.intValue())));
        addRenderableWidget(maxDamageTextsSlider);

        int contentBottom = Math.max(leftY, rightY) + buttonHeight;
        int buttonY = Math.min(contentBottom + 20, this.height - buttonHeight - 10);
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

        int itemsPerColumn = 7;
        int titleHeight = 20;
        int labelHeight = 20;
        int footerHeight = 40;
        int elementSpacing = 24;
        int contentHeight = titleHeight + labelHeight + (itemsPerColumn * elementSpacing) + footerHeight;

        int startY = Math.max(10, (this.height - contentHeight) / 2);
        int titleY = startY;
        int labelY = startY + titleHeight;

        graphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFFFFF);

        int centerX = this.width / 2;
        int columnWidth = 180;
        int columnSpacing = 40;
        int leftColumnX = centerX - columnWidth - columnSpacing / 2;
        int rightColumnX = centerX + columnSpacing / 2;

        graphics.drawString(this.font, Component.translatable("label.exile_overlay.display_settings"), leftColumnX,
                labelY, 0xAAAAAA);
        graphics.drawString(this.font, Component.translatable("label.exile_overlay.numeric_settings"), rightColumnX,
                labelY, 0xAAAAAA);

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
        config.setFontPreset(currentFontPreset);

        config.save();
        DamageFontRenderer.reloadCustomFont();
        this.minecraft.setScreen(parent);
    }

    private void onCancel() {
        LOGGER.info("Canceling damage popup config changes");
        this.minecraft.setScreen(parent);
    }

    private Component getFontPresetButtonText() {
        return Component.literal("Font: " + currentFontPreset.getDisplayName());
    }

    private void cycleFontPreset() {
        FontPreset[] presets = FontPreset.values();
        int currentIndex = currentFontPreset.ordinal();
        int nextIndex = (currentIndex + 1) % presets.length;
        currentFontPreset = presets[nextIndex];
        fontPresetButton.setMessage(getFontPresetButtonText());
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
