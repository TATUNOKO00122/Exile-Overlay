package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.client.config.position.Anchor;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.HudRenderManager;
import com.example.exile_overlay.client.render.effect.BuffOverlayRenderer;
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
 * 【機能】
 * - HUD要素をドラッグして位置変更
 * - リアルタイムプレビュー
 * - リセット機能
 * - リセット機能
 */
public class DraggableHudConfigScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(DraggableHudConfigScreen.class);

    private static final int BACKGROUND_COLOR = 0xCC000000;
    private static final int GRID_COLOR = 0x33FFFFFF;
    private static final int SELECTION_COLOR = 0xFFFFFF00;
    private static final int SNAP_GUIDE_COLOR = 0xFFFF5555;
    private static final int SNAP_GUIDE_ALPHA = 0x66;
    private static final int SNAP_DISTANCE = 10;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;
    private static final float SCALE_STEP = 0.1f;
    private static final int TOGGLE_BUTTON_MIN_SIZE = 5;
    private static final int TOGGLE_BUTTON_MAX_SIZE = 7;
    private static final int TOGGLE_BUTTON_COLOR_VISIBLE = 0xFF44FF44;
    private static final int TOGGLE_BUTTON_COLOR_HIDDEN = 0xFFFF4444;
    private static final int ORIENTATION_BUTTON_COLOR = 0xFF4444FF;
    private static final int ORIENTATION_BUTTON_COLOR_ACTIVE = 0xFFFF4444;
    private final Screen parent;
    private final List<DraggableElement> draggableElements;
    private final HudPositionManager positionManager;

    private DraggableElement draggedElement = null;
    private DraggableElement selectedElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // ドラッグ中の変更をメモリ上に保持（確定時に一括保存）
    private final Map<String, HudPosition> pendingChanges = new HashMap<>();

    // スナップガイド表示用
    private final List<Integer> activeSnapGuidesX = new ArrayList<>();
    private final List<Integer> activeSnapGuidesY = new ArrayList<>();

    private SnapCalculator snapCalculator;

    private Button resetButton;
    private Button resetAllButton;
    private Button doneButton;

    public DraggableHudConfigScreen(Screen parent) {
        super(Component.translatable("screen.exile_overlay.hud_config.title"));
        this.parent = parent;
        this.draggableElements = new ArrayList<>();
        this.positionManager = HudPositionManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        // スナップ計算機の初期化
        this.snapCalculator = new SnapCalculator(SNAP_DISTANCE, this.width, this.height);

        // 初期化
        positionManager.initialize();

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

        addRenderableWidget(resetButton);
        addRenderableWidget(resetAllButton);
        addRenderableWidget(doneButton);

        resetButton.active = false;
    }

    /**
     * ドラッグ可能な要素を登録
     * 実際のHUDレンダラーから自動的に収集する
     */
    private void registerDraggableElements() {
        draggableElements.clear();

        // 位置マネージャーに登録されている全てのキーに対応する要素を作成
        for (String key : positionManager.getDefaultPositions().keySet()) {
            // ダメージポップアップはHUDではないため、ドラッグ設定から除外
            if ("damage_popup".equals(key)) {
                continue;
            }

            HudPosition position = positionManager.getPosition(key);

            // 実際のレンダラーからサイズを取得
            IHudRenderer renderer = HudRenderManager.getInstance().getHudRenderer(key);
            int width;
            int height;

            if (renderer != null) {
                // 設定画面用のサイズを優先的に使用（動的サイズ対応）
                width = renderer.getConfigWidth();
                height = renderer.getConfigHeight();
                LOGGER.debug("Got size for '{}': {}x{} from renderer (config)", key, width, height);
            } else {
                // レンダラーが見つからない場合はIHudRendererのデフォルトフォールバックサイズを使用
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
        // 背景
        renderConfigBackground(graphics);

        // グリッド
        renderGrid(graphics);

        // スナップガイド
        renderSnapGuides(graphics);

        // HUD要素プレビュー
        renderHudPreviews(graphics, mouseX, mouseY);



        // タイトル
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        // ヘルプテキスト
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.exile_overlay.hud_config.help"),
                this.width / 2, 25, 0xAAAAAA);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.exile_overlay.hud_config.help_scale"),
                this.width / 2, 38, 0xAAAAAA);

        // 選択中要素の情報
        if (selectedElement != null) {

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
            int[] pos;

            if (element == draggedElement) {
                // ドラッグ中はスナップ後の保存済み位置を使用
                pos = element.getResolvedPosition(this.width, this.height);
            } else {
                pos = element.getResolvedPosition(this.width, this.height);
            }

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

        // スケーリング適用後のサイズ
        int width = (int) (baseWidth * scale);
        int height = (int) (baseHeight * scale);

        IHudRenderer.HudRenderMetadata metadata = element.getRenderMetadata();
        IHudRenderer.Insets offset = metadata.getOffset();
        IHudRenderer.Insets expansion = metadata.getExpansion();

        // スケーリングを考慮したexpansion
        int scaledExpansionH = (int) (expansion.getHorizontal() * scale);
        int scaledExpansionV = (int) (expansion.getVertical() * scale);
        int scaledOffsetLeft = (int) (offset.left * scale);
        int scaledOffsetTop = (int) (offset.top * scale);

        int left;
        int top;
        if (metadata.isTopLeftBased()) {
            left = x + scaledOffsetLeft;
            top = y + scaledOffsetTop;
        } else if (metadata.isBottomCenterBased()) {
            left = x - width / 2 - (int) (expansion.left * scale) + scaledOffsetLeft;
            top = y - height - (int) (expansion.top * scale) + scaledOffsetTop;
        } else {
            left = x - width / 2 - (int) (expansion.left * scale) + scaledOffsetLeft;
            top = y - height / 2 - (int) (expansion.top * scale) + scaledOffsetTop;
        }
        int renderWidth = width + scaledExpansionH;
        int renderHeight = height + scaledExpansionV;

        int baseAlpha = element == selectedElement ? 0x66 : 0x33;
        if (!isVisible) {
            baseAlpha = 0x22;
        }
        int color = (baseAlpha << 24) | (isVisible ? 0x4444FF : 0xFF4444);
        graphics.fill(left, top, left + renderWidth, top + renderHeight, color);

        graphics.renderOutline(left, top, renderWidth, renderHeight, isVisible ? 0xFFFFFFFF : 0xFF888888);

        renderToggleButton(graphics, element);
        renderOrientationButton(graphics, element);
    }

    private void renderToggleButton(GuiGraphics graphics, DraggableElement element) {
        int[] btnPos = element.getToggleButtonPosition(this.width, this.height);
        int btnSize = element.getToggleButtonSize();
        boolean isVisible = element.isVisible();

        int btnColor = isVisible ? TOGGLE_BUTTON_COLOR_VISIBLE : TOGGLE_BUTTON_COLOR_HIDDEN;
        graphics.fill(btnPos[0], btnPos[1], btnPos[0] + btnSize, btnPos[1] + btnSize, btnColor);
        graphics.renderOutline(btnPos[0], btnPos[1], btnSize, btnSize, 0xFFFFFFFF);


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
        graphics.renderOutline(btnPos[0], btnPos[1], btnSize, btnSize, 0xFFFFFFFF);
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

        if (button == 0) {
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

            DraggableElement hitElement = findElementAt((int) mouseX, (int) mouseY);

            if (hitElement != null) {
                selectedElement = hitElement;
                draggedElement = hitElement;

                int[] pos = hitElement.getResolvedPosition(this.width, this.height);
                dragOffsetX = (int) mouseX - pos[0];
                dragOffsetY = (int) mouseY - pos[1];

                resetButton.active = true;
                return true;
            } else {
                selectedElement = null;
                resetButton.active = false;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleElementVisibility(DraggableElement element) {
        boolean newVisible = !element.isVisible();
        HudPosition newPosition = element.getPosition().withVisible(newVisible);
        element.updatePosition(newPosition);
        positionManager.setPosition(element.getKey(), newPosition);
        pendingChanges.put(element.getKey(), newPosition);
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
            // ドラッグ終了: 新しい位置を保存
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
        IHudRenderer.HudRenderMetadata metadata = draggedElement.getRenderMetadata();

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
        HudPosition newPosition = HudPosition.fromAbsolute(result.x(), result.y(), this.width, this.height, currentScale, currentHorizontal);

        // リアルタイムで位置を更新（HUDが即座に移動）
        positionManager.setPosition(draggedElement.getKey(), newPosition);
        draggedElement.updatePosition(newPosition);
        
        // 変更したキーを記録（ファイル保存時に使用）
        pendingChanges.put(draggedElement.getKey(), newPosition);
    }

    /**
     * ドラッグを確定して位置を保存
     * スナップ機能付き
     */
    private void finalizeDrag(int mouseX, int mouseY) {
        if (draggedElement == null) return;

        int rawX = mouseX - dragOffsetX;
        int rawY = mouseY - dragOffsetY;

        // 要素のサイズと座標系を取得
        int elementWidth = draggedElement.getWidth();
        int elementHeight = draggedElement.getHeight();
        IHudRenderer.HudRenderMetadata metadata = draggedElement.getRenderMetadata();

        // スナップ計算
        SnapCalculator.SnapResult result = snapCalculator.calculateSnap(
            rawX, rawY, elementWidth, elementHeight, metadata
        );

        // スナップガイドを更新
        activeSnapGuidesX.clear();
        activeSnapGuidesX.addAll(result.guidesX());
        activeSnapGuidesY.clear();
        activeSnapGuidesY.addAll(result.guidesY());

        // 新しい位置を計算（現在のスケールと向きを保持）
        float currentScale = draggedElement.getPosition().getScale();
        boolean currentHorizontal = draggedElement.getPosition().isHorizontal();
        HudPosition newPosition = HudPosition.fromAbsolute(result.x(), result.y(), this.width, this.height, currentScale, currentHorizontal);

        // リアルタイムで位置を更新
        positionManager.setPosition(draggedElement.getKey(), newPosition);
        draggedElement.updatePosition(newPosition);

        // 変更したキーを記録
        pendingChanges.put(draggedElement.getKey(), newPosition);

        LOGGER.debug("Drag finalized for {}: {}", draggedElement.getKey(), newPosition);
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
            IHudRenderer renderer = HudRenderManager.getInstance().getHudRenderer(key);
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

        boolean isHit(int mouseX, int mouseY, int screenWidth, int screenHeight) {
            int[] pos = getResolvedPosition(screenWidth, screenHeight);
            float scale = position.getScale();
            int scaledWidth = (int) (baseWidth * scale);
            int scaledHeight = (int) (baseHeight * scale);

            // メタデータから設定を取得
            IHudRenderer renderer = HudRenderManager.getInstance().getHudRenderer(key);
            IHudRenderer.HudRenderMetadata metadata = renderer != null
                ? renderer.getRenderMetadata()
                : new IHudRenderer.HudRenderMetadata(
                    IHudRenderer.CoordinateSystem.CENTER_BASED,
                    new IHudRenderer.Insets(0, 0, 0, 0),
                    new IHudRenderer.Insets(0, 0, 0, 0)
                  );

            IHudRenderer.Insets offset = metadata.getOffset();
            IHudRenderer.Insets expansion = metadata.getExpansion();

            // スケーリングを適用
            int scaledOffsetLeft = (int) (offset.left * scale);
            int scaledOffsetTop = (int) (offset.top * scale);
            int scaledExpansionLeft = (int) (expansion.left * scale);
            int scaledExpansionTop = (int) (expansion.top * scale);
            int scaledExpansionH = (int) (expansion.getHorizontal() * scale);
            int scaledExpansionV = (int) (expansion.getVertical() * scale);

            int left;
            int top;
            if (metadata.isTopLeftBased()) {
                // 左上基準
                left = pos[0] + scaledOffsetLeft;
                top = pos[1] + scaledOffsetTop;
            } else if (metadata.isBottomCenterBased()) {
                // 底辺中心基準: Xは中心、Yは底辺（ホットバー等の下部配置要素用）
                left = pos[0] - scaledWidth / 2 - scaledExpansionLeft + scaledOffsetLeft;
                top = pos[1] - scaledHeight - scaledExpansionTop + scaledOffsetTop;
            } else {
                // 中心基準: XとYの両方が中心
                left = pos[0] - scaledWidth / 2 - scaledExpansionLeft + scaledOffsetLeft;
                top = pos[1] - scaledHeight / 2 - scaledExpansionTop + scaledOffsetTop;
            }
            int right = left + scaledWidth + scaledExpansionH;
            int bottom = top + scaledHeight + scaledExpansionV;

            return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
        }

        IHudRenderer.HudRenderMetadata getRenderMetadata() {
            IHudRenderer renderer = HudRenderManager.getInstance().getHudRenderer(key);
            if (renderer != null) {
                return renderer.getRenderMetadata();
            }
            return new IHudRenderer.HudRenderMetadata(
                IHudRenderer.CoordinateSystem.CENTER_BASED,
                new IHudRenderer.Insets(0, 0, 0, 0),
                new IHudRenderer.Insets(0, 0, 0, 0)
            );
        }

        boolean isVisible() {
            return position.isVisible();
        }

        boolean isHorizontal() {
            return position.isHorizontal();
        }

        boolean supportsOrientation() {
            return "skill_hotbar".equals(key);
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
            int scaledWidth = (int) (baseWidth * scale);
            int scaledHeight = (int) (baseHeight * scale);
            int btnSize = getToggleButtonSize();

            IHudRenderer.HudRenderMetadata metadata = getRenderMetadata();
            IHudRenderer.Insets offset = metadata.getOffset();
            IHudRenderer.Insets expansion = metadata.getExpansion();

            // スケーリングを適用
            int scaledOffsetLeft = (int) (offset.left * scale);
            int scaledOffsetTop = (int) (offset.top * scale);
            int scaledExpansionLeft = (int) (expansion.left * scale);
            int scaledExpansionTop = (int) (expansion.top * scale);

            int left;
            int top;
            if (metadata.isTopLeftBased()) {
                left = pos[0] + scaledOffsetLeft;
                top = pos[1] + scaledOffsetTop;
            } else if (metadata.isBottomCenterBased()) {
                left = pos[0] - scaledWidth / 2 - scaledExpansionLeft + scaledOffsetLeft;
                top = pos[1] - scaledHeight - scaledExpansionTop + scaledOffsetTop;
            } else {
                left = pos[0] - scaledWidth / 2 - scaledExpansionLeft + scaledOffsetLeft;
                top = pos[1] - scaledHeight / 2 - scaledExpansionTop + scaledOffsetTop;
            }

            return new int[]{left, top};
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
