package com.example.exile_overlay.client.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HUD設定の一覧画面
 *
 * 【機能】
 * - HUD位置設定画面への遷移
 * - 今後の拡張用メニュー
 */
public class HudListScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(HudListScreen.class);

    private static final int BACKGROUND_COLOR = 0xCC000000;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 8;

    private final Screen parent;

    private Button hudPositionButton;
    private Button damagePopupButton;
    // private Button mobHealthBarButton; // DISABLED: 3D HPBar
    private Button doneButton;

    public HudListScreen(Screen parent) {
        super(Component.translatable("screen.exile_overlay.hud_list.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int buttonsCount = 4;
        int titleHeight = 20;
        int labelSpacing = 20;
        int totalHeight = titleHeight + labelSpacing + (BUTTON_HEIGHT + BUTTON_SPACING) * buttonsCount;

        int startY = Math.max(10, (this.height - totalHeight) / 2);
        int widgetStartY = startY + titleHeight + labelSpacing;

        // HUD位置設定ボタン
        hudPositionButton = Button.builder(
                Component.translatable("button.exile_overlay.hud_position"),
                button -> openHudPositionScreen())
                .bounds(centerX - BUTTON_WIDTH / 2, widgetStartY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        // Damageポップアップ設定ボタン
        damagePopupButton = Button.builder(
                Component.translatable("button.exile_overlay.damage_popup_config"),
                button -> openDamagePopupConfigScreen())
                .bounds(centerX - BUTTON_WIDTH / 2, widgetStartY + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH,
                        BUTTON_HEIGHT)
                .build();

        // MOBヘルスバー設定ボタン
        // DISABLED: 3D HPBar
        // mobHealthBarButton = Button.builder(
        //         Component.translatable("button.exile_overlay.mob_healthbar_config"),
        //         button -> openMobHealthBarConfigScreen())
        //         .bounds(centerX - BUTTON_WIDTH / 2, widgetStartY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, BUTTON_WIDTH,
        //                 BUTTON_HEIGHT)
        //         .build();

        // 完了ボタン
        doneButton = Button.builder(
                Component.translatable("button.exile_overlay.done"),
                button -> onDone())
                .bounds(centerX - BUTTON_WIDTH / 2, widgetStartY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, BUTTON_WIDTH,
                        BUTTON_HEIGHT)
                .build();

        addRenderableWidget(hudPositionButton);
        addRenderableWidget(damagePopupButton);
        // addRenderableWidget(mobHealthBarButton); // DISABLED: 3D HPBar
        addRenderableWidget(doneButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 背景
        renderScreenBackground(graphics);

        int buttonsCount = 4;
        int titleHeight = 20;
        int labelSpacing = 20;
        int totalHeight = titleHeight + labelSpacing + (BUTTON_HEIGHT + BUTTON_SPACING) * buttonsCount;
        int startY = Math.max(10, (this.height - totalHeight) / 2);

        // タイトル
        graphics.drawCenteredString(this.font, this.title, this.width / 2, startY, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 背景を描画
     */
    private void renderScreenBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
    }

    /**
     * HUD位置設定画面を開く
     */
    private void openHudPositionScreen() {
        LOGGER.info("Opening HUD position configuration screen");
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new DraggableHudConfigScreen(this));
    }

    /**
     * Damageポップアップ設定画面を開く
     */
    private void openDamagePopupConfigScreen() {
        LOGGER.info("Opening damage popup configuration screen");
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new DamagePopupConfigScreen(this));
    }

    // DISABLED: 3D HPBar
    // /**
    //  * MOBヘルスバー設定画面を開く
    //  */
    // private void openMobHealthBarConfigScreen() {
    //     LOGGER.info("Opening mob health bar configuration screen");
    //     Minecraft mc = Minecraft.getInstance();
    //     mc.setScreen(new MobHealthBarConfigScreen(this));
    // }

    /**
     * 完了ボタン処理
     */
    private void onDone() {
        LOGGER.info("Closing HUD list screen");
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
