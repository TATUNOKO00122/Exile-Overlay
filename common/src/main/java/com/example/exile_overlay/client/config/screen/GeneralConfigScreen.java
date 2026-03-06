package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    private final Screen parent;
    private final EquipmentDisplayConfig config;

    private Checkbox usePercentageCheckbox;
    private Checkbox enableShadowCheckbox;

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
        int itemCount = 3;
        int contentHeight = titleHeight + labelHeight + (itemCount * elementSpacing) + footerHeight;

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

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderScreenBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
    }

    private void onSave() {
        LOGGER.info("Saving equipment display config");

        config.setUsePercentage(usePercentageCheckbox.selected());
        config.setEnableShadow(enableShadowCheckbox.selected());
        config.save();

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
}
