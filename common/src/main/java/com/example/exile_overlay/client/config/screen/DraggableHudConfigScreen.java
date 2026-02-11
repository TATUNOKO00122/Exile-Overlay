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
            int width = 80;
            int height = 40;

            if (renderer != null) {
                // 設定画面用のサイズを優先的に使用（動的サイズ対応）
                width = renderer.getConfigWidth();
                height = renderer.getConfigHeight();
                LOGGER.debug("Got size for '{}': {}x{} from renderer (config)", key, width, height);
            } else {
                // レンダラーが見つからない場合はフォールバックサイズを使用
                int[] fallbackSize = getFallbackSize(key);
                width = fallbackSize[0];
                height = fallbackSize[1];
                LOGGER.debug("Using fallback size for '{}': {}x{}", key, width, height);
            }

            DraggableElement element = new DraggableElement(key, position, width, height);
            draggableElements.add(element);
        }

        LOGGER.debug("Registered {} draggable elements", draggableElements.size());
    }

    /**
     * レンダラーが見つからない場合のフォールバックサイズ
     */
    private int[] getFallbackSize(String key) {
        // キーに基づいて適切なサイズを返す
        switch (key) {
            case "hotbar":
                return new int[]{230, 60}; // HotbarRenderCommandのサイズ
            case "damage_popup":
                return new int[]{100, 60};
            case "buff_overlay":
                return new int[]{30, 39}; // テクスチャサイズと同じ
            default:
                return new int[]{80, 40};
        }
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

        // 選択中要素の情報
        if (selectedElement != null) {
            renderElementInfo(graphics, selectedElement);
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
    private long lastDebugLogTime = 0;

    private void renderHudPreviews(GuiGraphics graphics, int mouseX, int mouseY) {
        for (DraggableElement element : draggableElements) {
            int[] pos;

            if (element == draggedElement) {
                // ドラッグ中はマウス位置に追従
                pos = new int[]{mouseX - dragOffsetX, mouseY - dragOffsetY};
            } else {
                pos = element.getResolvedPosition(this.width, this.height);
            }

            // デバッグ：バフ要素の位置を2秒ごとにログ出力
            if ("buff_overlay".equals(element.getKey())) {
                long now = System.currentTimeMillis();
                if (now - lastDebugLogTime > 2000) {
                    Minecraft mc = Minecraft.getInstance();
                    int mcWidth = mc.getWindow().getGuiScaledWidth();
                    int mcHeight = mc.getWindow().getGuiScaledHeight();
                    LOGGER.debug("[CONFIG] Buff preview position: ({}, {}), screen: {}x{}, mc: {}x{}", 
                        pos[0], pos[1], this.width, this.height, mcWidth, mcHeight);
                    lastDebugLogTime = now;
                }
            }

            renderElementPreview(graphics, element, pos[0], pos[1]);
        }
    }

    /**
     * 単一要素のプレビューを描画
     * メタデータに基づいて座標計算
     */
    private void renderElementPreview(GuiGraphics graphics, DraggableElement element, int x, int y) {
        int width = element.getWidth();
        int height = element.getHeight();

        // メタデータから設定を取得
        IHudRenderer.HudRenderMetadata metadata = element.getRenderMetadata();
        IHudRenderer.Insets offset = metadata.getOffset();
        IHudRenderer.Insets expansion = metadata.getExpansion();

        int left;
        int top;
        if (metadata.isTopLeftBased()) {
            // 左上基準: x, yがそのまま左上の座標
            left = x + offset.left;
            top = y + offset.top;
        } else if (metadata.isBottomCenterBased()) {
            // 底辺中心基準: Xは中心、Yは底辺（ホットバー等の下部配置要素用）
            left = x - width / 2 - expansion.left + offset.left;
            top = y - height - expansion.top + offset.top;
        } else {
            // 中心基準: XとYの両方が中心
            left = x - width / 2 - expansion.left + offset.left;
            top = y - height / 2 - expansion.top + offset.top;
        }
        int renderWidth = width + expansion.getHorizontal();
        int renderHeight = height + expansion.getVertical();

        // 背景（半透明）
        int alpha = element == selectedElement ? 0x44 : 0x22;
        int color = (alpha << 24) | 0x4444FF;
        graphics.fill(left, top, left + renderWidth, top + renderHeight, color);

        // 枠線
        graphics.renderOutline(left, top, renderWidth, renderHeight, 0xFFFFFFFF);

        // 要素名
        String name = element.getDisplayName();
        int textX = metadata.isTopLeftBased() ? left + renderWidth / 2 : x;
        int textY = top + renderHeight / 2 - 4;
        graphics.drawCenteredString(this.font, name, textX, textY, 0xFFFFFF);

        // 位置情報（小さく）
        String posInfo = String.format("(%d, %d)", x, y);
        int infoY = top + renderHeight - 10;
        graphics.drawCenteredString(this.font, posInfo, textX, infoY, 0xAAAAAA);
    }

    /**
     * 要素情報を描画
     */
    private void renderElementInfo(GuiGraphics graphics, DraggableElement element) {
        int[] pos = element.getResolvedPosition(this.width, this.height);
        HudPosition hudPos = element.getPosition();

        String[] info = {
                "Element: " + element.getKey(),
                "Position: (" + pos[0] + ", " + pos[1] + ")",
                "Anchor: " + hudPos.getAnchor().name(),
                "Offset: (" + hudPos.getOffsetX() + ", " + hudPos.getOffsetY() + ")"
        };

        int infoX = 10;
        int infoY = 50;

        for (int i = 0; i < info.length; i++) {
            graphics.drawString(this.font, info[i], infoX, infoY + i * 12, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // ドラッグ開始時にスナップガイドをリセット
        activeSnapGuidesX.clear();
        activeSnapGuidesY.clear();

        if (button == 0) {
            // 左クリック: 要素選択/ドラッグ開始
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

    /**
     * ドラッグ中の位置をリアルタイムに更新
     * メモリ上のみ保持（ファイルには書き込まない）
     * スナップ機能付き：画面枠に近づくと自動的に吸着
     */
    private void updateDragPosition(int mouseX, int mouseY) {
        if (draggedElement == null) return;

        // スナップガイドをリセット
        activeSnapGuidesX.clear();
        activeSnapGuidesY.clear();

        int rawX = mouseX - dragOffsetX;
        int rawY = mouseY - dragOffsetY;

        // 要素のサイズを取得
        int elementWidth = draggedElement.getWidth();
        int elementHeight = draggedElement.getHeight();

        // スナップ処理：表示位置（rawX/Y）をそのままスナップ対象に
        int snappedX = applySnapX(rawX, elementWidth);
        int snappedY = applySnapY(rawY, elementHeight);

        HudPosition newPosition = HudPosition.fromAbsolute(snappedX, snappedY, this.width, this.height);

        // 即座に保存（リアルタイム反映のため）
        positionManager.setPosition(draggedElement.getKey(), newPosition);
        draggedElement.updatePosition(newPosition);
    }

    /**
     * X座標にスナップを適用
     * rawXは表示位置の基準点（中心または左上）
     */
    private int applySnapX(int rawX, int elementWidth) {
        // 画面左端スナップ（表示位置が画面左端に）
        if (Math.abs(rawX) <= SNAP_DISTANCE) {
            activeSnapGuidesX.add(0);
            return 0;
        }

        // 画面右端スナップ（表示位置が画面右端に）
        // rawXは表示位置の基準点なので、右端スナップ時は画面幅に合わせる
        if (Math.abs(rawX - this.width) <= SNAP_DISTANCE) {
            activeSnapGuidesX.add(this.width);
            return this.width;
        }

        return rawX;
    }

    /**
     * Y座標にスナップを適用
     * rawYは表示位置の基準点（中心または左上）
     */
    private int applySnapY(int rawY, int elementHeight) {
        // 画面上端スナップ（表示位置が画面上端に）
        if (Math.abs(rawY) <= SNAP_DISTANCE) {
            activeSnapGuidesY.add(0);
            return 0;
        }

        // 画面下端スナップ（表示位置が画面下端に）
        if (Math.abs(rawY - this.height) <= SNAP_DISTANCE) {
            activeSnapGuidesY.add(this.height);
            return this.height;
        }

        return rawY;
    }

    /**
     * ドラッグを確定して位置を保存
     * スナップ機能付き
     */
    private void finalizeDrag(int mouseX, int mouseY) {
        if (draggedElement == null) return;

        // スナップガイドをリセット
        activeSnapGuidesX.clear();
        activeSnapGuidesY.clear();

        int rawX = mouseX - dragOffsetX;
        int rawY = mouseY - dragOffsetY;

        // 要素のサイズを取得
        int elementWidth = draggedElement.getWidth();
        int elementHeight = draggedElement.getHeight();

        // スナップ処理を適用：表示位置（rawX/Y）をそのままスナップ対象に
        int snappedX = applySnapX(rawX, elementWidth);
        int snappedY = applySnapY(rawY, elementHeight);

        // 新しい位置を計算
        HudPosition newPosition = HudPosition.fromAbsolute(snappedX, snappedY, this.width, this.height);

        // 即座に保存（リアルタイム反映のため）
        positionManager.setPosition(draggedElement.getKey(), newPosition);
        draggedElement.updatePosition(newPosition);

        LOGGER.debug("Drag finalized and saved for {}: {}", draggedElement.getKey(), newPosition);
    }

    /**
     * 保留中の変更を全て確定して保存
     */
    private void commitPendingChanges() {
        if (pendingChanges.isEmpty()) return;

        LOGGER.info("Committing {} pending position changes", pendingChanges.size());

        for (Map.Entry<String, HudPosition> entry : pendingChanges.entrySet()) {
            positionManager.setPosition(entry.getKey(), entry.getValue());
            LOGGER.debug("Committed position for {}", entry.getKey());
        }

        pendingChanges.clear();
    }

    /**
     * 指定座標にある要素を検索
     */
    private DraggableElement findElementAt(int x, int y) {
        // 後ろから検索して、前面の要素を優先
        for (int i = draggableElements.size() - 1; i >= 0; i--) {
            DraggableElement element = draggableElements.get(i);
            if (element.isHit(x, y, this.width, this.height)) {
                return element;
            }
        }
        return null;
    }

    /**
     * 選択中の要素をリセット
     */
    private void resetSelectedElement() {
        if (selectedElement != null) {
            positionManager.resetToDefault(selectedElement.getKey());
            selectedElement.updatePosition(positionManager.getPosition(selectedElement.getKey()));
            LOGGER.debug("Reset position for {}", selectedElement.getKey());
        }
    }

    /**
     * 全ての要素をリセット
     */
    private void resetAllElements() {
        positionManager.resetAllToDefaults();
        for (DraggableElement element : draggableElements) {
            element.updatePosition(positionManager.getPosition(element.getKey()));
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
        private final int width;
        private final int height;

        DraggableElement(String key, HudPosition position, int width, int height) {
            this.key = key;
            this.position = position;
            this.width = width;
            this.height = height;
        }

        String getKey() {
            return key;
        }

        String getDisplayName() {
            // キーから表示名を生成
            return key.replace("_", " ").toUpperCase();
        }

        HudPosition getPosition() {
            return position;
        }

        void updatePosition(HudPosition newPosition) {
            this.position = newPosition;
        }

        int[] getResolvedPosition(int screenWidth, int screenHeight) {
            return position.resolve(screenWidth, screenHeight);
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }

        boolean isHit(int mouseX, int mouseY, int screenWidth, int screenHeight) {
            int[] pos = getResolvedPosition(screenWidth, screenHeight);

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

            int left;
            int top;
            if (metadata.isTopLeftBased()) {
                // 左上基準
                left = pos[0] + offset.left;
                top = pos[1] + offset.top;
            } else if (metadata.isBottomCenterBased()) {
                // 底辺中心基準: Xは中心、Yは底辺（ホットバー等の下部配置要素用）
                left = pos[0] - width / 2 - expansion.left + offset.left;
                top = pos[1] - height - expansion.top + offset.top;
            } else {
                // 中心基準: XとYの両方が中心
                left = pos[0] - width / 2 - expansion.left + offset.left;
                top = pos[1] - height / 2 - expansion.top + offset.top;
            }
            int right = left + width + expansion.getHorizontal();
            int bottom = top + height + expansion.getVertical();

            return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
        }

        IHudRenderer.HudRenderMetadata getRenderMetadata() {
            IHudRenderer renderer = HudRenderManager.getInstance().getHudRenderer(key);
            if (renderer != null) {
                return renderer.getRenderMetadata();
            }
            // デフォルト: 中心基準
            return new IHudRenderer.HudRenderMetadata(
                IHudRenderer.CoordinateSystem.CENTER_BASED,
                new IHudRenderer.Insets(0, 0, 0, 0),
                new IHudRenderer.Insets(0, 0, 0, 0)
            );
        }
    }
}
