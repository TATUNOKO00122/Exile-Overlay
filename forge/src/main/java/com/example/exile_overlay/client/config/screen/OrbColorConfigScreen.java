package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.client.config.OrbColorConfig;
import com.example.exile_overlay.client.render.HudRenderManager;
import com.example.exile_overlay.client.render.orb.OrbDummyPreviewManager;
import com.example.exile_overlay.client.render.orb.OrbDummyPreviewManager.OrbTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ゲーム内オーブカラー調整画面（幅調整＆設定画面風半透明パネル対応）
 */
public class OrbColorConfigScreen extends Screen {

    private final Screen parent;
    private final OrbColorConfig colorConfig;
    private final OrbDummyPreviewManager previewManager;
    private final List<OrbTarget> activeTargets;
    private int currentTargetIndex = 0;

    private final Map<OrbTarget, Integer> initialColors = new HashMap<>();

    private ColorSlider redSlider;
    private ColorSlider greenSlider;
    private ColorSlider blueSlider;

    private boolean isUpdatingFromCode = false;

    public OrbColorConfigScreen(Screen parent) {
        super(Component.translatable("screen.exile_overlay.orb_color_config.title"));
        this.parent = parent;
        this.colorConfig = OrbColorConfig.getInstance();
        this.previewManager = OrbDummyPreviewManager.getInstance();
        this.activeTargets = previewManager.getActiveTargets();

        for (OrbTarget target : OrbTarget.values()) {
            initialColors.put(target, target.getColor(colorConfig));
        }
    }

    private OrbTarget getCurrentTarget() {
        if (activeTargets.isEmpty()) return OrbTarget.HEALTH;
        if (currentTargetIndex < 0 || currentTargetIndex >= activeTargets.size()) {
            currentTargetIndex = 0;
        }
        return activeTargets.get(currentTargetIndex);
    }

    @Override
    protected void init() {
        super.init();
        this.previewManager.setDummyPreviewActive(true);

        int panelW = 220;
        int panelH = 170;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        int currentY = panelY + 10;

        // 1. 1つの切り替えボタン (クリックで対象変更)
        Button targetToggleButton = Button.builder(getTargetButtonMessage(), btn -> {
            currentTargetIndex = (currentTargetIndex + 1) % activeTargets.size();
            updateSlidersFromCurrentColor();
            this.resetScreenWidgets();
        }).bounds(panelX + 15, currentY, panelW - 30, 20).build();
        this.addRenderableWidget(targetToggleButton);

        currentY += 40; // HEX描画スペースを考慮して空ける

        // 2. RGBスライダー（幅をつまみがはみ出さないサイズに調整）
        int argb = getCurrentTarget().getColor(colorConfig);
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        int sliderW = panelW - 30;
        int sliderX = panelX + 15;

        redSlider = new ColorSlider(sliderX, currentY, sliderW, 18, "screen.exile_overlay.color.red", r, val -> onColorChannelChanged());
        this.addRenderableWidget(redSlider);
        currentY += 22;

        greenSlider = new ColorSlider(sliderX, currentY, sliderW, 18, "screen.exile_overlay.color.green", g, val -> onColorChannelChanged());
        this.addRenderableWidget(greenSlider);
        currentY += 22;

        blueSlider = new ColorSlider(sliderX, currentY, sliderW, 18, "screen.exile_overlay.color.blue", b, val -> onColorChannelChanged());
        this.addRenderableWidget(blueSlider);
        currentY += 24;

        // 3. 完了ボタン
        Button doneBtn = Button.builder(CommonComponents.GUI_DONE, btn -> {
            saveAndClose();
        }).bounds(panelX + 15, currentY, panelW - 30, 20).build();
        this.addRenderableWidget(doneBtn);
    }

    private void resetScreenWidgets() {
        this.clearWidgets();
        this.init();
    }

    private Component getTargetButtonMessage() {
        OrbTarget target = getCurrentTarget();
        return Component.translatable("screen.exile_overlay.orb_color.target", target.getDisplayName());
    }

    private void onColorChannelChanged() {
        if (isUpdatingFromCode) return;

        OrbTarget target = getCurrentTarget();
        int currentArgb = target.getColor(colorConfig);
        int alpha = (currentArgb >> 24) & 0xFF;
        if (alpha == 0) alpha = 0xFF;

        int r = redSlider.getValueInt();
        int g = greenSlider.getValueInt();
        int b = blueSlider.getValueInt();

        int newColor = (alpha << 24) | (r << 16) | (g << 8) | b;
        target.setColor(colorConfig, newColor);
    }

    private void updateSlidersFromCurrentColor() {
        int color = getCurrentTarget().getColor(colorConfig);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        isUpdatingFromCode = true;
        if (redSlider != null) redSlider.setValueFromInt(r);
        if (greenSlider != null) greenSlider.setValueFromInt(g);
        if (blueSlider != null) blueSlider.setValueFromInt(b);
        isUpdatingFromCode = false;
    }

    private void saveAndClose() {
        colorConfig.save();
        this.previewManager.setDummyPreviewActive(false);
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. 実際のHUD（オーブ、ダミー状態含む）を背景に直接描画
        HudRenderManager.getInstance().render(graphics, this.width, this.height);

        // 2. 設定画面と同じモダンダーク半透明背景 (0xCC000000)
        int panelW = 220;
        int panelH = 170;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC000000);

        // カラーコード(HEX)表示 & カラープレビュー領域の描画
        int argb = getCurrentTarget().getColor(colorConfig);
        int rgb = argb & 0xFFFFFF;
        String hexCode = String.format("#%06X", rgb);
        Component hexComponent = Component.translatable("screen.exile_overlay.color.hex", hexCode);

        int hexY = panelY + 35;
        int previewBoxSize = 10;
        int textW = this.font.width(hexComponent);
        int startX = (this.width - (textW + previewBoxSize + 6)) / 2;

        // カラーチッププレビュー
        graphics.fill(startX, hexY, startX + previewBoxSize, hexY + previewBoxSize, 0xFF000000);
        graphics.fill(startX + 1, hexY + 1, startX + previewBoxSize - 1, hexY + previewBoxSize - 1, 0xFF000000 | rgb);

        // HEX テキスト
        graphics.drawString(this.font, hexComponent, startX + previewBoxSize + 6, hexY + 1, 0xFFDDDDDD, false);

        // 3. UIコンポーネント（ボタン、スライダー）の描画
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * ローカライズ対応RGBスライダー
     */
    private static class ColorSlider extends AbstractSliderButton {
        private final String channelTranslationKey;
        private final java.util.function.Consumer<Integer> valueConsumer;

        public ColorSlider(int x, int y, int width, int height, String channelTranslationKey, int initialValue, java.util.function.Consumer<Integer> valueConsumer) {
            super(x, y, width, height, Component.translatable(channelTranslationKey, initialValue), initialValue / 255.0);
            this.channelTranslationKey = channelTranslationKey;
            this.valueConsumer = valueConsumer;
        }

        public int getValueInt() {
            return (int) Math.round(this.value * 255.0);
        }

        public void setValueFromInt(int val) {
            int clamped = Math.max(0, Math.min(255, val));
            this.value = clamped / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable(channelTranslationKey, getValueInt()));
        }

        @Override
        protected void applyValue() {
            valueConsumer.accept(getValueInt());
        }
    }
}
