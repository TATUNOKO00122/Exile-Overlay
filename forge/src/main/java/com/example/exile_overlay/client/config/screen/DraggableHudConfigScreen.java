package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.client.config.position.Anchor;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.HudRenderManager;
import com.example.exile_overlay.client.render.effect.BuffOverlayRenderer;
import com.example.exile_overlay.dmgtracker.config.TrackerConfig;
import com.example.exile_overlay.dmgtracker.network.TrackerSyncS2C;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD位置のドラッグ・ドロップ設定画面
 *
 * - HUD要素をドラッグして位置変更
 * - リアルタイムプレビュー
 * - リセット機能
 */
public class DraggableHudConfigScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(DraggableHudConfigScreen.class);

    private static final int BACKGROUND_COLOR = 0x1A000000;
    private static final int GRID_COLOR = 0x33FFFFFF;
    private static final int SELECTION_COLOR = 0xFFFFFF00;
    private static final int SNAP_GUIDE_COLOR = 0xFFFF5555;
    private static final int SNAP_GUIDE_ALPHA = 0x66;
    private static final int SNAP_DISTANCE = 10;
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 4.0f;
    private static final float SCALE_STEP = 0.1f;
    private static final int TOGGLE_BUTTON_MIN_SIZE = 5;
    private static final int TOGGLE_BUTTON_MAX_SIZE = 7;
    private static final int TOGGLE_BUTTON_COLOR_VISIBLE = 0xFF44FF44;
    private static final int TOGGLE_BUTTON_COLOR_HIDDEN = 0xFFFF4444;
    private static final int ORIENTATION_BUTTON_COLOR = 0xFF4444FF;
    private static final int ORIENTATION_BUTTON_COLOR_ACTIVE = 0xFFFF4444;

    private static final long HELP_DISPLAY_MS = 3000L;
    private static final long HELP_FADE_MS = 2000L;
    private static final int HELP_TEXT_COLOR = 0xAAAAAA;
    private final Screen parent;
    private final List<DraggableElement> draggableElements;
    private final HudPositionManager positionManager;

    private DraggableElement draggedElement = null;
    private DraggableElement selectedElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int dragStartX = 0;
    private int dragStartY = 0;

    private long helpStartTime;
    private boolean helpDismissed = false;

    // ドラッグ中の変更をメモリ上に保持（確定時に一括保存）
    private final Map<String, HudPosition> pendingChanges = new HashMap<>();

    // スナップガイド表示用
    private final List<Integer> activeSnapGuidesX = new ArrayList<>();
    private final List<Integer> activeSnapGuidesY = new ArrayList<>();

    private SnapCalculator snapCalculator;

    private Button resetButton;
    private Button resetAllButton;
    private Button colorButton;
    private Button doneButton;
    private Button helpToggleButton;

    public DraggableHudConfigScreen(Screen parent) {
        super(Component.translatable("screen.exile_overlay.hud_config.title"));
        this.parent = parent;
        this.draggableElements = new ArrayList<>();
        this.positionManager = HudPositionManager.getInstance();
        this.helpStartTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();

        // スナップ計算機の初期化
        this.snapCalculator = new SnapCalculator(SNAP_DISTANCE, this.width, this.height);

        // 初期化
        positionManager.initialize();

        draggedElement = null;
        selectedElement = null;

        // ドラッグ可能な要素を登録
        registerDraggableElements();

        // ボタン配置
        int buttonY = this.height - 30;

        resetButton = Button.builder(
                        Component.translatable("button.exile_overlay.reset"),
                        button -> resetSelectedElement())
                .bounds(10, buttonY, 80, 20)
                .build();

        resetAllButton = Button.builder(
                        Component.translatable("button.exile_overlay.reset_all"),
                        button -> resetAllElements())
                .bounds(100, buttonY, 80, 20)
                .build();

        doneButton = Button.builder(
                        Component.translatable("button.exile_overlay.done"),
                        button -> onDone())
                .bounds(this.width - 90, buttonY, 80, 20)
                .build();

        colorButton = Button.builder(
                        Component.translatable("button.exile_overlay.colors"),
                        button -> this.minecraft.setScreen(new OrbColorConfigScreen(this)))
                .bounds(190, buttonY, 80, 20)
                .build();

        addRenderableWidget(resetButton);
        addRenderableWidget(resetAllButton);
        addRenderableWidget(colorButton);
        addRenderableWidget(doneButton);

        resetButton.active = false;

        helpToggleButton = Button.builder(
                        Component.literal("?"),
                        button -> resetHelpTimer())
                .bounds(this.width - 25, 5, 20, 20)
                .build();
        addRenderableWidget(helpToggleButton);
    }

    /**
     * ドラッグ可能な要素を登録
     * 実際のHUDレンダラーから自動的に収集する
     */
    private void registerDraggableElements() {
        draggableElements.clear();

        for (String key : positionManager.getDefaultPositions().keySet()) {
            if ("damage_popup".equals(key) || "day_counter".equals(key)) {
                continue;
            }

            if ("skill_hotbar".equals(key) || "buff_overlay".equals(key) || "skill_buff_overlay".equals(key)) {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("mmorpg")) continue;
            }

            if ("skill_buff_overlay".equals(key)) {
                HudPosition pos = positionManager.getPosition(key);
                if (!pos.isVisible()) continue;
            }

            if ("damage_tracker".equals(key)) {
                if (!TrackerSyncS2C.ClientTrackerData.serverHasMod() || !TrackerConfig.getInstance().isEnabled()) continue;
            }

            if ("gateway_boss_bar".equals(key)) {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("gateways")) continue;
            }

            if ("botania_mana_bar".equals(key)) {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("botania")) continue;
            }

            if ("lightmans_currency_coins".equals(key)) {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("lightmanscurrency")) continue;
            }

            HudPosition position = positionManager.getPosition(key);

            IRenderCommand renderer = HudRenderManager.getInstance().getHudRenderer(key);
            int width;
            int height;

            if (renderer != null) {
                width = renderer.getConfigWidth();
                height = renderer.getConfigHeight();
                LOGGER.debug("Got size for '{}': {}x{} from renderer (config)", key, width, height);
            } else {
                int[] fallbackSize = new int[]{80, 40};
                width = fallbackSize[0];
                height = fallbackSize[1];
                LOGGER.debug("Using default fallback size for '{}': {}x{}", key, width, height);
            }

            DraggableElement element = new DraggableElement(key, position, width, height);
            draggableElements.add(element);
        }

        LOGGER.debug("Registered {} draggable elements", draggableElements.size());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean isDragging = draggedElement != null;

        // ドラッグ中はすべての操作ボタンを一時的に非表示にする
        if (resetButton != null) resetButton.visible = !isDragging;
        if (resetAllButton != null) resetAllButton.visible = !isDragging;
        if (doneButton != null) doneButton.visible = !isDragging;
        if (colorButton != null) colorButton.visible = !isDragging;
        if (helpToggleButton != null) helpToggleButton.visible = !isDragging;

        // 背景
        renderConfigBackground(graphics);

        // グリッド
        renderGrid(graphics);

        // スナップガイド
        renderSnapGuides(graphics);

        // HUD要素プレビュー
        renderHudPreviews(graphics, mouseX, mouseY);

        // ヘルプテキスト（ドラッグ中以外で表示）
        if (!isDragging) {
            int helpAlpha = calculateHelpAlpha();
            if (helpAlpha > 0) {
                int helpColor = (helpAlpha << 24) | HELP_TEXT_COLOR;
                graphics.drawCenteredString(this.font,
                        Component.translatable("screen.exile_overlay.hud_config.help"),
                        this.width / 2, 10, helpColor);
                graphics.drawCenteredString(this.font,
                        Component.translatable("screen.exile_overlay.hud_config.help_scale"),
                        this.width / 2, 23, helpColor);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 背景を描画
     */
    private void renderConfigBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
    }

    /**
     * グリッドを描画（現在は無効化）
     */
    private void renderGrid(GuiGraphics graphics) {
        // グリッド描画は無効化されています
    }

    /**
     * スナップガイドを描画
     * ドラッグ中にスナップ位置を視覚的に表示
     */
    private void renderSnapGuides(GuiGraphics graphics) {
        if (draggedElement == null) return;
        if (activeSnapGuidesX.isEmpty() && activeSnapGuidesY.isEmpty()) return;

        int guideColor = (SNAP_GUIDE_ALPHA << 24) | (SNAP_GUIDE_COLOR & 0xFFFFFF);

        // 垂直スナップガイド
        for (int x : activeSnapGuidesX) {
            graphics.fill(x - 1, 0, x + 2, this.height, guideColor);
        }

        // 水平スナップガイド
        for (int y : activeSnapGuidesY) {
            graphics.fill(0, y - 1, this.width, y + 2, guideColor);
        }
    }

    /**
     * HUD要素のプレビューを描画
     */
    private void renderHudPreviews(GuiGraphics graphics, int mouseX, int mouseY) {
        for (DraggableElement element : draggableElements) {
            int[] pos = element.getResolvedPosition(this.width, this.height);
            renderElementPreview(graphics, element, pos[0], pos[1]);
        }
    }

    /**
     * 単一要素のプレビューを描画
     * メタデータに基づいて座標計算
     */
    private void renderElementPreview(GuiGraphics graphics, DraggableElement element, int x, int y) {
        int baseWidth = element.getBaseWidth();
        int baseHeight = element.getBaseHeight();
        float scale = element.getScale();
        boolean isVisible = element.isVisible();

        IRenderCommand.HudRenderMetadata metadata = element.getRenderMetadata();
        int[] bounds = DraggableElement.calculateBounds(x, y, baseWidth, baseHeight, scale, metadata);

        int left = bounds[0];
        int top = bounds[1];
        int renderWidth = bounds[2];
        int renderHeight = bounds[3];

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int baseAlpha = element == selectedElement ? 0x66 : 0x33;
        if (!isVisible) {
            baseAlpha = 0x22;
        }
        int color = (baseAlpha << 24) | (isVisible ? 0x4444FF : 0xFF4444);
        graphics.fill(left, top, left + renderWidth, top + renderHeight, color);

        graphics.renderOutline(left, top, renderWidth, renderHeight, isVisible ? 0x88FFFFFF : 0xFF888888);

        String key = element.getKey();
        if ("boss_hp_bar".equals(key) || "gateway_boss_bar".equals(key) || "damage_tracker".equals(key)) {
            renderElementLabel(graphics, element, left, top, renderWidth, renderHeight);
        }

        renderToggleButton(graphics, element);
        renderOrientationButton(graphics, element);

        RenderSystem.disableBlend();
    }

    private void renderElementLabel(GuiGraphics graphics, DraggableElement element,
                                     int left, int top, int width, int height) {
        var font = Minecraft.getInstance().font;
        Component label = element.getDisplayName();
        int labelWidth = font.width(label);
        int labelHeight = font.lineHeight;

        float labelScale = Math.min(
                (float) width * 0.9f / labelWidth,
                (float) height * 0.9f / labelHeight
        );
        labelScale = Math.min(labelScale, 1.0f);

        int centerX = left + width / 2;
        int centerY = top + height / 2;
        int drawX = centerX - (int) (labelWidth * labelScale) / 2;
        int drawY = centerY - (int) (labelHeight * labelScale) / 2;

        var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(drawX, drawY, 400);
        poseStack.scale(labelScale, labelScale, 1.0f);
        graphics.drawString(font, label, 0, 0, 0xFFFFFFFF, true);
        poseStack.popPose();
    }

    private void renderToggleButton(GuiGraphics graphics, DraggableElement element) {
        int[] btnPos = element.getToggleButtonPosition(this.width, this.height);
        int btnSize = element.getToggleButtonSize();
        boolean isVisible = element.isVisible();

        int btnColor = isVisible ? TOGGLE_BUTTON_COLOR_VISIBLE : TOGGLE_BUTTON_COLOR_HIDDEN;
        graphics.fill(btnPos[0], btnPos[1], btnPos[0] + btnSize, btnPos[1] + btnSize, btnColor);
        graphics.renderOutline(btnPos[0], btnPos[1], btnSize, btnSize, 0x88FFFFFF);
    }

    private void renderOrientationButton(GuiGraphics graphics, DraggableElement element) {
        if (!element.supportsOrientation()) {
            return;
        }

        int[] btnPos = element.getOrientationButtonPosition(this.width, this.height);
        int btnSize = element.getOrientationButtonSize();
        boolean isHorizontal = element.isHorizontal();

        int btnColor = isHorizontal ? ORIENTATION_BUTTON_COLOR_ACTIVE : ORIENTATION_BUTTON_COLOR;
        graphics.fill(btnPos[0], btnPos[1], btnPos[0] + btnSize, btnPos[1] + btnSize, btnColor);
        graphics.renderOutline(btnPos[0], btnPos[1], btnSize, btnSize, 0x88FFFFFF);
    }

    private int calculateHelpAlpha() {
        if (helpDismissed) return 0;
        long elapsed = System.currentTimeMillis() - helpStartTime;
        if (elapsed < HELP_DISPLAY_MS) return 0xFF;
        if (elapsed >= HELP_DISPLAY_MS + HELP_FADE_MS) return 0;
        float progress = (float) (elapsed - HELP_DISPLAY_MS) / HELP_FADE_MS;
        return Math.round(0xFF * (1.0f - progress));
    }

    private void dismissHelp() {
        helpDismissed = true;
    }

    private void resetHelpTimer() {
        helpStartTime = System.currentTimeMillis();
        helpDismissed = false;
    }

    /**
     * 要素情報を描画
     */
    private void renderElementInfo(GuiGraphics graphics, DraggableElement element) {
        int[] pos = element.getResolvedPosition(this.width, this.height);
        HudPosition hudPos = element.getPosition();

        Component[] info = {
                Component.translatable("hud.exile_overlay.element.element", element.getKey()),
                Component.translatable("hud.exile_overlay.element.position", pos[0], pos[1]),
                Component.translatable("hud.exile_overlay.element.anchor", hudPos.getAnchor().name()),
                Component.translatable("hud.exile_overlay.element.offset", hudPos.getOffsetX(), hudPos.getOffsetY()),
                Component.translatable("hud.exile_overlay.element.scale", hudPos.getScale())
        };

        int infoX = 10;
        int infoY = 50;

        for (int i = 0; i < info.length; i++) {
            graphics.drawString(this.font, info[i].getString(), infoX, infoY + i * 12, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        activeSnapGuidesX.clear();
        activeSnapGuidesY.clear();
        dismissHelp();

        if (mouseY >= this.height - 35) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (button == 0) {
            DraggableElement hitElement = findElementAt((int) mouseX, (int) mouseY);

            if (hitElement != null) {
                selectedElement = hitElement;
                draggedElement = hitElement;

                int[] pos = hitElement.getResolvedPosition(this.width, this.height);
                dragOffsetX = (int) mouseX - pos[0];
                dragOffsetY = (int) mouseY - pos[1];
                dragStartX = (int) mouseX;
                dragStartY = (int) mouseY;

                resetButton.active = true;
                return true;
            }

            for (int i = draggableElements.size() - 1; i >= 0; i--) {
                DraggableElement element = draggableElements.get(i);
                if (element.isToggleButtonHit((int) mouseX, (int) mouseY, this.width, this.height)) {
                    toggleElementVisibility(element);
                    return true;
                }
                if (element.isOrientationButtonHit((int) mouseX, (int) mouseY, this.width, this.height)) {
                    toggleElementOrientation(element);
                    return true;
                }
            }

            selectedElement = null;
            resetButton.active = false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleElementVisibility(DraggableElement element) {
        boolean newVisible = !element.isVisible();
        HudPosition newPosition = element.getPosition().withVisible(newVisible);
        element.updatePosition(newPosition);
        positionManager.setPosition(element.getKey(), newPosition);
        pendingChanges.put(element.getKey(), newPosition);
        if ("damage_tracker".equals(element.getKey())) {
            com.example.exile_overlay.dmgtracker.config.TrackerConfig.setShowOverlay(newVisible);
        }
        LOGGER.debug("Toggled visibility for {}: {}", element.getKey(), newVisible);
    }

    private void toggleElementOrientation(DraggableElement element) {
        boolean newHorizontal = !element.isHorizontal();
        HudPosition newPosition = element.getPosition().withHorizontal(newHorizontal);
        element.updatePosition(newPosition);
        positionManager.setPosition(element.getKey(), newPosition);
        pendingChanges.put(element.getKey(), newPosition);
        
        // 向き切り替え後にサイズを再計算（動的サイズ対応）
        element.refreshSize();
        
        LOGGER.debug("Toggled orientation for {}: {}", element.getKey(), newHorizontal ? "horizontal" : "vertical");
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggedElement != null) {
            int dx = (int) mouseX - dragStartX;
            int dy = (int) mouseY - dragStartY;
            if (dx * dx + dy * dy <= 9) {
                if (draggedElement.isToggleButtonHit(dragStartX, dragStartY, this.width, this.height)) {
                    toggleElementVisibility(draggedElement);
                    draggedElement = null;
                    return true;
                }
                if (draggedElement.isOrientationButtonHit(dragStartX, dragStartY, this.width, this.height)) {
                    toggleElementOrientation(draggedElement);
                    draggedElement = null;
                    return true;
                }
            }
            finalizeDrag((int) mouseX, (int) mouseY);
            draggedElement = null;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggedElement != null && button == 0) {
            updateDragPosition((int) mouseX, (int) mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        dismissHelp();

        // マウスが要素の上にあるかチェック
        DraggableElement hoveredElement = findElementAt((int) mouseX, (int) mouseY);
        
        if (hoveredElement != null) {
            // 要素を選択状態にする
            selectedElement = hoveredElement;
            resetButton.active = true;
            
            // スケール変更
            float currentScale = hoveredElement.getScale();
            float newScale;
            
            if (delta > 0) {
                // スクロールアップ: 拡大
                newScale = Math.min(currentScale + SCALE_STEP, MAX_SCALE);
            } else {
                // スクロールダウン: 縮小
                newScale = Math.max(currentScale - SCALE_STEP, MIN_SCALE);
            }
            
            if (newScale != currentScale) {
                HudPosition newPosition = hoveredElement.getPosition().withScale(newScale);
                hoveredElement.updatePosition(newPosition);
                positionManager.setPosition(hoveredElement.getKey(), newPosition);
                pendingChanges.put(hoveredElement.getKey(), newPosition);
                hoveredElement.refreshSize();

                LOGGER.debug("Scale changed for {}: {} -> {}", hoveredElement.getKey(), currentScale, newScale);
            }
            
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * ドラッグ中の位置をリアルタイムに更新
     * メモリ上の位置マネージャーを即座に更新（HUDはリアルタイムで移動）
     * ファイルへの保存は画面終了時に一括実行
     */
    private void updateDragPosition(int mouseX, int mouseY) {
        if (draggedElement == null) return;

        int rawX = mouseX - dragOffsetX;
        int rawY = mouseY - dragOffsetY;

        // 要素のサイズを取得
        int elementWidth = draggedElement.getWidth();
        int elementHeight = draggedElement.getHeight();

        // 座標系を取得
        IRenderCommand.HudRenderMetadata metadata = draggedElement.getRenderMetadata();

        // スナップ計算
        SnapCalculator.SnapResult result = snapCalculator.calculateSnap(
            rawX, rawY, elementWidth, elementHeight, metadata
        );

        // スナップガイドを更新
        activeSnapGuidesX.clear();
        activeSnapGuidesX.addAll(result.guidesX());
        activeSnapGuidesY.clear();
        activeSnapGuidesY.addAll(result.guidesY());

        // 現在のスケールと向きを保持して位置を更新
        float currentScale = draggedElement.getPosition().getScale();
        boolean currentHorizontal = draggedElement.getPosition().isHorizontal();
        boolean currentVisible = draggedElement.getPosition().isVisible();
        HudPosition newPosition = HudPosition.fromAbsolute(result.x(), result.y(), this.width, this.height, currentScale, currentVisible, currentHorizontal);

        // リアルタイムで位置を更新（HUDが即座に移動）
        positionManager.setPosition(draggedElement.getKey(), newPosition);
        draggedElement.updatePosition(newPosition);
        
        // 変更したキーを記録（ファイル保存時に使用）
        pendingChanges.put(draggedElement.getKey(), newPosition);
    }

    /**
     * ドラッグを確定して位置を保存
     * 最後のupdateDragPositionの結果をそのまま使用（再計算による丸め誤差を回避）
     */
    private void finalizeDrag(int mouseX, int mouseY) {
        if (draggedElement == null) return;

        activeSnapGuidesX.clear();
        activeSnapGuidesY.clear();

        LOGGER.debug("Drag finalized for {}: {}", draggedElement.getKey(), draggedElement.getPosition());
    }

    /**
     * 変更をファイルに保存
     * 実際の位置更新はリアルタイムで行われているため、ここではファイル保存のみ実行
     */
    private void commitPendingChanges() {
        if (pendingChanges.isEmpty()) {
            LOGGER.debug("No pending changes to save");
            return;
        }

        LOGGER.info("Saving {} position changes to file", pendingChanges.size());
        positionManager.saveToFile();
        pendingChanges.clear();
    }

    /**
     * 指定座標にある要素を検索
     */
    private DraggableElement findElementAt(int x, int y) {
        DraggableElement smallest = null;
        int smallestArea = Integer.MAX_VALUE;

        for (int i = 0; i < draggableElements.size(); i++) {
            DraggableElement element = draggableElements.get(i);
            if (element.isHit(x, y, this.width, this.height)) {
                int area = element.getWidth() * element.getHeight();
                if (area < smallestArea) {
                    smallestArea = area;
                    smallest = element;
                }
            }
        }
        return smallest;
    }

    /**
     * 選択中の要素をリセット
     */
    private void resetSelectedElement() {
        if (selectedElement != null) {
            String key = selectedElement.getKey();
            positionManager.resetToDefault(key);
            HudPosition defaultPosition = positionManager.getPosition(key);
            selectedElement.updatePosition(defaultPosition);
            pendingChanges.put(key, defaultPosition);
            LOGGER.debug("Reset position for {}", key);
        }
    }

    /**
     * 全ての要素をリセット
     */
    private void resetAllElements() {
        positionManager.resetAllToDefaults();
        for (DraggableElement element : draggableElements) {
            String key = element.getKey();
            HudPosition defaultPosition = positionManager.getPosition(key);
            element.updatePosition(defaultPosition);
            pendingChanges.put(key, defaultPosition);
        }
        LOGGER.debug("Reset all positions to defaults");
    }

    /**
     * 完了ボタン処理
     */
    private void onDone() {
        commitPendingChanges();
        positionManager.saveToFile();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        commitPendingChanges();
        positionManager.saveToFile();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        if (draggedElement != null) {
            finalizeDrag(
                draggedElement.getResolvedPosition(this.width, this.height)[0] + dragOffsetX,
                draggedElement.getResolvedPosition(this.width, this.height)[1] + dragOffsetY
            );
            draggedElement = null;
        }
        selectedElement = null;
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * ドラッグ可能な要素を表すクラス
     */
    private static class DraggableElement {
        private final String key;
        private HudPosition position;
        private int baseWidth;
        private int baseHeight;

        DraggableElement(String key, HudPosition position, int width, int height) {
            this.key = key;
            this.position = position;
            this.baseWidth = width;
            this.baseHeight = height;
        }

        /**
         * レンダラーから最新のサイズを再取得
         * 向き切り替え時や動的サイズ変更時に呼び出す
         */
        void refreshSize() {
            IRenderCommand renderer = HudRenderManager.getInstance().getHudRenderer(key);
            if (renderer != null) {
                this.baseWidth = renderer.getConfigWidth();
                this.baseHeight = renderer.getConfigHeight();
            }
        }

        String getKey() {
            return key;
        }

        Component getDisplayName() {
            return Component.translatable("hud.exile_overlay." + key);
        }

        HudPosition getPosition() {
            return position;
        }

        void updatePosition(HudPosition newPosition) {
            this.position = newPosition;
        }

        float getScale() {
            return position.getScale();
        }

        int[] getResolvedPosition(int screenWidth, int screenHeight) {
            return position.resolve(screenWidth, screenHeight);
        }

        int getWidth() {
            return (int) (baseWidth * position.getScale());
        }

        int getHeight() {
            return (int) (baseHeight * position.getScale());
        }

        int getBaseWidth() {
            return baseWidth;
        }

        int getBaseHeight() {
            return baseHeight;
        }

        /**
         * スケール適用済みのバウンディングボックスを計算。
         * renderElementPreview / isHit / getToggleButtonPosition で共通利用。
         *
         * @return int[4] = { left, top, renderWidth, renderHeight }
         */
        static int[] calculateBounds(
                int anchorX, int anchorY,
                int baseWidth, int baseHeight,
                float scale,
                IRenderCommand.HudRenderMetadata metadata) {

            int scaledWidth = (int) (baseWidth * scale);
            int scaledHeight = (int) (baseHeight * scale);

            IRenderCommand.Insets offset = metadata.getOffset();
            IRenderCommand.Insets expansion = metadata.getExpansion();

            int scaledOffsetLeft = (int) (offset.left * scale);
            int scaledOffsetTop = (int) (offset.top * scale);
            int scaledExpansionLeft = (int) (expansion.left * scale);
            int scaledExpansionTop = (int) (expansion.top * scale);
            int scaledExpansionH = (int) (expansion.getHorizontal() * scale);
            int scaledExpansionV = (int) (expansion.getVertical() * scale);

            int left;
            int top;
            if (metadata.isTopLeftBased()) {
                left = anchorX + scaledOffsetLeft;
                top = anchorY + scaledOffsetTop;
            } else if (metadata.isBottomCenterBased()) {
                left = anchorX - scaledWidth / 2 - scaledExpansionLeft + scaledOffsetLeft;
                top = anchorY - scaledHeight - scaledExpansionTop + scaledOffsetTop;
            } else {
                left = anchorX - scaledWidth / 2 - scaledExpansionLeft + scaledOffsetLeft;
                top = anchorY - scaledHeight / 2 - scaledExpansionTop + scaledOffsetTop;
            }

            int renderWidth = scaledWidth + scaledExpansionH;
            int renderHeight = scaledHeight + scaledExpansionV;

            return new int[]{left, top, renderWidth, renderHeight};
        }

        boolean isHit(int mouseX, int mouseY, int screenWidth, int screenHeight) {
            int[] pos = getResolvedPosition(screenWidth, screenHeight);
            float scale = position.getScale();

            IRenderCommand renderer = HudRenderManager.getInstance().getHudRenderer(key);
            IRenderCommand.HudRenderMetadata metadata = renderer != null
                ? renderer.getRenderMetadata()
                : new IRenderCommand.HudRenderMetadata(
                    IRenderCommand.CoordinateSystem.CENTER_BASED,
                    new IRenderCommand.Insets(0, 0, 0, 0),
                    new IRenderCommand.Insets(0, 0, 0, 0)
                  );

            int[] bounds = calculateBounds(pos[0], pos[1], baseWidth, baseHeight, scale, metadata);

            // 当たり判定のパディング拡張（Botaniaなどの細い要素向け）
            int padding = 3;
            int hitLeft = bounds[0] - padding;
            int hitTop = bounds[1] - padding;
            int hitRight = bounds[0] + bounds[2] + padding;
            int hitBottom = bounds[1] + bounds[3] + padding;

            // 最小ヒットエリア（高さが狭すぎる場合に上下に拡張）
            int minHitHeight = 12;
            int currentHitHeight = hitBottom - hitTop;
            if (currentHitHeight < minHitHeight) {
                int extra = (minHitHeight - currentHitHeight) / 2;
                hitTop -= extra;
                hitBottom += extra;
            }

            return mouseX >= hitLeft && mouseX < hitRight && mouseY >= hitTop && mouseY < hitBottom;
        }

        IRenderCommand.HudRenderMetadata getRenderMetadata() {
            IRenderCommand renderer = HudRenderManager.getInstance().getHudRenderer(key);
            if (renderer != null) {
                return renderer.getRenderMetadata();
            }
            return new IRenderCommand.HudRenderMetadata(
                IRenderCommand.CoordinateSystem.CENTER_BASED,
                new IRenderCommand.Insets(0, 0, 0, 0),
                new IRenderCommand.Insets(0, 0, 0, 0)
            );
        }

        boolean isVisible() {
            return position.isVisible();
        }

        boolean isHorizontal() {
            return position.isHorizontal();
        }

        boolean supportsOrientation() {
            return "skill_hotbar".equals(key) || "minion_overlay".equals(key);
        }

        boolean supportsToggleButton() {
            return true;
        }

        int getToggleButtonSize() {
            float scale = position.getScale();
            int size = (int) (Math.min(baseWidth, baseHeight) * scale * 0.3f);
            return Math.max(TOGGLE_BUTTON_MIN_SIZE, Math.min(TOGGLE_BUTTON_MAX_SIZE, size));
        }

        int getOrientationButtonSize() {
            return getToggleButtonSize();
        }

        int[] getToggleButtonPosition(int screenWidth, int screenHeight) {
            int[] pos = getResolvedPosition(screenWidth, screenHeight);
            float scale = position.getScale();
            IRenderCommand.HudRenderMetadata metadata = getRenderMetadata();

            int[] bounds = calculateBounds(pos[0], pos[1], baseWidth, baseHeight, scale, metadata);
            return new int[]{bounds[0], bounds[1]};
        }

        boolean isToggleButtonHit(int mouseX, int mouseY, int screenWidth, int screenHeight) {
            int[] btnPos = getToggleButtonPosition(screenWidth, screenHeight);
            int btnSize = getToggleButtonSize();
            return mouseX >= btnPos[0] && mouseX < btnPos[0] + btnSize &&
                   mouseY >= btnPos[1] && mouseY < btnPos[1] + btnSize;
        }

        int[] getOrientationButtonPosition(int screenWidth, int screenHeight) {
            int[] togglePos = getToggleButtonPosition(screenWidth, screenHeight);
            int btnSize = getOrientationButtonSize();
            return new int[]{togglePos[0] + btnSize + 1, togglePos[1]};
        }

        boolean isOrientationButtonHit(int mouseX, int mouseY, int screenWidth, int screenHeight) {
            if (!supportsOrientation()) {
                return false;
            }
            int[] btnPos = getOrientationButtonPosition(screenWidth, screenHeight);
            int btnSize = getOrientationButtonSize();
            return mouseX >= btnPos[0] && mouseX < btnPos[0] + btnSize &&
                   mouseY >= btnPos[1] && mouseY < btnPos[1] + btnSize;
        }
    }
}
