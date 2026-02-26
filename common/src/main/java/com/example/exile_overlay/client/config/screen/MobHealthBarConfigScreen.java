package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.client.config.MobHealthBarConfig;
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
 * MOBヘルスバー設定画面
 *
 * 【機能】
 * - 表示/非表示の切り替え
 * - プレイヤーへの表示設定
 * - バーサイズ設定
 */
public class MobHealthBarConfigScreen extends Screen {

        private static final Logger LOGGER = LoggerFactory.getLogger(MobHealthBarConfigScreen.class);

        private static final int BACKGROUND_COLOR = 0xCC000000;
        private static final int BUTTON_WIDTH = 150;
        private static final int BUTTON_HEIGHT = 20;
        private static final int SPACING = 22;
        private static final int COLUMN_SPACING = 40;

        private final Screen parent;
        private final MobHealthBarConfig config;

        // チェックボックス
        private Checkbox showHealthBarCheckbox;
        private Checkbox showPlayerHealthBarCheckbox;
        private Checkbox showForAllMobsCheckbox;
        private Checkbox showOnlyWhenDamagedCheckbox;
        private Checkbox showHealthValueCheckbox;
        private Checkbox showMaxHealthCheckbox;
        private Checkbox showPercentageCheckbox;
        private Checkbox showThroughWallsCheckbox;

        // スライダー
        private SliderButton barWidthSlider;
        private SliderButton barHeightSlider;
        private SliderButton scaleSlider;
        private SliderButton curveAmountSlider;
        private SliderButton verticalOffsetSlider;
        private SliderButton horizontalOffsetSlider;

        public MobHealthBarConfigScreen(Screen parent) {
                super(Component.translatable("screen.exile_overlay.mob_healthbar_config.title"));
                this.parent = parent;
                this.config = MobHealthBarConfig.getInstance();
        }

        @Override
        protected void init() {
                super.init();

                int centerX = this.width / 2;
                int columnWidth = 180;
                int leftColumnX = centerX - columnWidth - COLUMN_SPACING / 2;
                int rightColumnX = centerX + COLUMN_SPACING / 2;

                int itemsPerColumn = 7;
                int titleHeight = 20;
                int labelHeight = 20;
                int footerHeight = 40;
                int contentHeight = titleHeight + labelHeight + (itemsPerColumn * SPACING) + footerHeight;

                int startY = Math.max(10, (this.height - contentHeight) / 2);
                int widgetStartY = startY + titleHeight + labelHeight;

                // === 左カラム：表示設定 ===
                int leftY = widgetStartY;

                showHealthBarCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("checkbox.exile_overlay.show_healthbar"),
                                config.isShowHealthBar());
                addRenderableWidget(showHealthBarCheckbox);
                leftY += SPACING;

                showPlayerHealthBarCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("checkbox.exile_overlay.show_player_healthbar"),
                                config.isShowPlayerHealthBar());
                addRenderableWidget(showPlayerHealthBarCheckbox);
                leftY += SPACING;

                showForAllMobsCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("checkbox.exile_overlay.show_for_all_mobs"),
                                config.isShowForAllMobs());
                addRenderableWidget(showForAllMobsCheckbox);
                leftY += SPACING;

                showOnlyWhenDamagedCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("checkbox.exile_overlay.show_only_when_damaged"),
                                config.isShowOnlyWhenDamaged());
                addRenderableWidget(showOnlyWhenDamagedCheckbox);
                leftY += SPACING;

                showHealthValueCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("checkbox.exile_overlay.show_health_value"),
                                config.isShowHealthValue());
                addRenderableWidget(showHealthValueCheckbox);
                leftY += SPACING;

                showMaxHealthCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("checkbox.exile_overlay.show_max_health"),
                                config.isShowMaxHealth());
                addRenderableWidget(showMaxHealthCheckbox);
                leftY += SPACING;

                showPercentageCheckbox = new Checkbox(leftColumnX, leftY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("checkbox.exile_overlay.show_percentage"),
                                config.isShowPercentage());
                addRenderableWidget(showPercentageCheckbox);

                // === 右カラム：サイズ設定 ===
                int rightY = widgetStartY;

                barWidthSlider = new SliderButton(rightColumnX, rightY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("slider.exile_overlay.bar_width"),
                                config.getBarWidth(), 10.0, 40.0, 1.0,
                                value -> Component.literal(String.valueOf(value.intValue()) + " px"));
                addRenderableWidget(barWidthSlider);
                rightY += SPACING;

                barHeightSlider = new SliderButton(rightColumnX, rightY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("slider.exile_overlay.bar_height"),
                                config.getBarHeight(), 1.0, 6.0, 0.5,
                                value -> Component.literal(String.format("%.1f", value) + " px"));
                addRenderableWidget(barHeightSlider);
                rightY += SPACING;

                scaleSlider = new SliderButton(rightColumnX, rightY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("slider.exile_overlay.bar_scale"),
                                config.getScale(), 0.5, 2.0, 0.1,
                                value -> Component.literal(String.format("%.1f", value) + "x"));
                addRenderableWidget(scaleSlider);
                rightY += SPACING;

                curveAmountSlider = new SliderButton(rightColumnX, rightY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("slider.exile_overlay.curve_amount"),
                                config.getCurveAmount(), 0.0, 1.0, 0.05,
                                value -> Component.literal(String.format("%.0f", value * 100) + "%"));
                addRenderableWidget(curveAmountSlider);
                rightY += SPACING;

                verticalOffsetSlider = new SliderButton(rightColumnX, rightY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("slider.exile_overlay.vertical_offset"),
                                config.getVerticalOffset(), -1.0, 3.0, 0.1,
                                value -> Component.literal(String.format("%.1f", value)));
                addRenderableWidget(verticalOffsetSlider);
                rightY += SPACING;

                horizontalOffsetSlider = new SliderButton(rightColumnX, rightY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("slider.exile_overlay.horizontal_offset"),
                                config.getHorizontalOffset(), -2.0, 2.0, 0.1,
                                value -> Component.literal(String.format("%.1f", value)));
                addRenderableWidget(horizontalOffsetSlider);
                rightY += SPACING;

                showThroughWallsCheckbox = new Checkbox(rightColumnX, rightY, columnWidth, BUTTON_HEIGHT,
                                Component.translatable("checkbox.exile_overlay.show_through_walls"),
                                config.isShowThroughWalls());
                addRenderableWidget(showThroughWallsCheckbox);

                // === ボタン ===
                int contentBottom = Math.max(leftY, rightY) + BUTTON_HEIGHT;
                int buttonY = Math.min(contentBottom + 20, this.height - BUTTON_HEIGHT - 10);
                int actionButtonWidth = 150;

                Button saveButton = Button.builder(
                                Component.translatable("button.exile_overlay.save"),
                                button -> onSave())
                                .bounds(centerX - actionButtonWidth - 10, buttonY, actionButtonWidth, BUTTON_HEIGHT)
                                .build();

                Button cancelButton = Button.builder(
                                Component.translatable("button.exile_overlay.cancel"),
                                button -> onCancel())
                                .bounds(centerX + 10, buttonY, actionButtonWidth, BUTTON_HEIGHT)
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
                int contentHeight = titleHeight + labelHeight + (itemsPerColumn * SPACING) + footerHeight;

                int startY = Math.max(10, (this.height - contentHeight) / 2);
                int titleY = startY;
                int labelY = startY + titleHeight;

                graphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFFFFF);

                int centerX = this.width / 2;
                int columnWidth = 180;
                int leftColumnX = centerX - columnWidth - COLUMN_SPACING / 2;
                int rightColumnX = centerX + COLUMN_SPACING / 2;

                graphics.drawString(this.font, Component.translatable("label.exile_overlay.display_settings"),
                                leftColumnX, labelY, 0xAAAAAA);
                graphics.drawString(this.font, Component.translatable("label.exile_overlay.position_settings"),
                                rightColumnX, labelY, 0xAAAAAA);

                super.render(graphics, mouseX, mouseY, partialTick);
        }

        private void renderScreenBackground(GuiGraphics graphics) {
                graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
        }

        private void onSave() {
                LOGGER.info("Saving mob health bar config");

                config.setShowHealthBar(showHealthBarCheckbox.selected());
                config.setShowPlayerHealthBar(showPlayerHealthBarCheckbox.selected());
                config.setShowForAllMobs(showForAllMobsCheckbox.selected());
                config.setShowOnlyWhenDamaged(showOnlyWhenDamagedCheckbox.selected());
                config.setShowHealthValue(showHealthValueCheckbox.selected());
                config.setShowMaxHealth(showMaxHealthCheckbox.selected());
                config.setShowPercentage(showPercentageCheckbox.selected());
                config.setShowThroughWalls(showThroughWallsCheckbox.selected());

                config.setBarWidth((float) barWidthSlider.getValue());
                config.setBarHeight((float) barHeightSlider.getValue());
                config.setScale((float) scaleSlider.getValue());
                config.setCurveAmount((float) curveAmountSlider.getValue());
                config.setVerticalOffset((float) verticalOffsetSlider.getValue());
                config.setHorizontalOffset((float) horizontalOffsetSlider.getValue());

                config.save();
                this.minecraft.setScreen(parent);
        }

        private void onCancel() {
                LOGGER.info("Canceling mob health bar config changes");
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
