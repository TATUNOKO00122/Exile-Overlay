package com.example.exile_overlay.api;

import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUDレンダラーの基底インターフェース
 *
 * 【依存性逆転の原則】
 * - 全てのHUD要素はこのインターフェースを実装
 * - レンダリングエンジンは実装の詳細を知らない
 */
public interface IHudRenderer {

    /**
     * HUD要素を描画
     *
     * @param graphics GUIグラフィックスコンテキスト
     * @param ctx レンダリングコンテキスト
     */
    void render(GuiGraphics graphics, RenderContext ctx);

    /**
     * このレンダラーが有効かどうか
     * HudPositionのvisible設定とRenderContextの状態をチェック
     *
     * @param ctx レンダリングコンテキスト
     * @return 描画すべき場合true
     */
    default boolean isVisible(RenderContext ctx) {
        return getPosition().isVisible();
    }

    /**
     * レンダラーの優先度（値が高いほど手前に描画）
     */
    default int getPriority() {
        return 100;
    }

    /**
     * レンダラーの一意なID
     */
    String getId();

    /**
     * 設定キー（位置設定で使用）
     * デフォルトではgetId()と同じ値を返す
     *
     * @return 設定ファイルでのキー名
     */
    default String getConfigKey() {
        return getId();
    }

    /**
     * このレンダラーの幅（ドラッグ用ヒット領域）
     *
     * @return 幅（ピクセル）
     */
    int getWidth();

    /**
     * このレンダラーの高さ（ドラッグ用ヒット領域）
     *
     * @return 高さ（ピクセル）
     */
    int getHeight();

    /**
     * 設定画面で使用する幅を取得
     * 動的サイズの要素（バフ等）はここで実際の表示サイズを返す
     *
     * @return 設定画面での幅（ピクセル）
     */
    default int getConfigWidth() {
        return getWidth();
    }

    /**
     * 設定画面で使用する高さを取得
     * 動的サイズの要素（バフ等）はここで実際の表示サイズを返す
     *
     * @return 設定画面での高さ（ピクセル）
     */
    default int getConfigHeight() {
        return getHeight();
    }

    /**
     * ドラッグによる位置変更を許可するか
     *
     * @return ドラッグ可能な場合true
     */
    default boolean isDraggable() {
        return false;
    }

    /**
     * 現在の位置設定を取得
     *
     * @return HUD位置設定
     */
    default HudPosition getPosition() {
        return HudPositionManager.getInstance().getPosition(getConfigKey());
    }

    /**
     * 現在のスケールを取得
     *
     * @return 表示スケール（1.0 = 100%）
     */
    default float getScale() {
        return getPosition().getScale();
    }

    /**
     * 現在の位置を解決した絶対座標を取得
     *
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     * @return [x, y] 配列
     */
    default int[] resolvePosition(int screenWidth, int screenHeight) {
        return getPosition().resolve(screenWidth, screenHeight);
    }

    /**
     * ヒットテスト（ドラッグ時の当たり判定）
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     * @return ヒットした場合true
     */
    default boolean isHit(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int[] pos = resolvePosition(screenWidth, screenHeight);
        if (pos == null || pos.length < 2) return false;
        
        int x = pos[0];
        int y = pos[1];
        int width = getWidth();
        int height = getHeight();

        HudRenderMetadata metadata = getRenderMetadata();
        if (metadata == null) return false;
        
        Insets offset = metadata.getOffset();
        Insets expansion = metadata.getExpansion();
        if (offset == null) offset = new Insets(0, 0, 0, 0);
        if (expansion == null) expansion = new Insets(0, 0, 0, 0);

        int left;
        int top;
        if (metadata.isTopLeftBased()) {
            left = x + offset.left;
            top = y + offset.top;
        } else if (metadata.isBottomCenterBased()) {
            left = x - width / 2 - expansion.left + offset.left;
            top = y - height - expansion.top + offset.top;
        } else {
            left = x - width / 2 - expansion.left + offset.left;
            top = y - height / 2 - expansion.top + offset.top;
        }
        int right = left + width + expansion.getHorizontal();
        int bottom = top + height + expansion.getVertical();

        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    /**
     * ドラッグ中のプレビュー位置を取得
     * ドラッグ中は一時的な位置を使用する
     *
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     * @param dragOffsetX ドラッグによるXオフセット
     * @param dragOffsetY ドラッグによるYオフセット
     * @return [x, y] 配列
     */
    default int[] getDragPreviewPosition(int screenWidth, int screenHeight, int dragOffsetX, int dragOffsetY) {
        int[] basePos = resolvePosition(screenWidth, screenHeight);
        return new int[]{basePos[0] + dragOffsetX, basePos[1] + dragOffsetY};
    }

    /**
     * HUD要素のレンダリングメタデータを取得
     * 設定画面での表示方法を制御する
     *
     * @return レンダリングメタデータ
     */
    default HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
            CoordinateSystem.CENTER_BASED,
            new Insets(0, 0, 0, 0),
            new Insets(0, 0, 0, 0)
        );
    }

    /**
     * フォールバックサイズを取得
     * レンダラーが設定画面で見つからない場合に使用される
     *
     * @return [width, height] 配列
     */
    default int[] getFallbackSize() {
        return new int[]{80, 40};
    }

    /**
     * 座標系の種類
     */
    enum CoordinateSystem {
        /** 中心基準（x, yが矩形の中心） */
        CENTER_BASED,
        /** 左上基準（x, yが矩形の左上） */
        TOP_LEFT_BASED,
        /** 底辺中心基準（xが中心、yが底辺）- ホットバー等の下部配置要素用 */
        BOTTOM_CENTER_BASED
    }

    /**
     * インセット（余白）を表すクラス
     */
    class Insets {
        public final int top;
        public final int right;
        public final int bottom;
        public final int left;

        public Insets(int top, int right, int bottom, int left) {
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.left = left;
        }

        public int getHorizontal() {
            return left + right;
        }

        public int getVertical() {
            return top + bottom;
        }
    }

    /**
     * HUDレンダリングメタデータ
     */
    class HudRenderMetadata {
        private final CoordinateSystem coordinateSystem;
        private final Insets offset;
        private final Insets expansion;

        public HudRenderMetadata(CoordinateSystem coordinateSystem, Insets offset, Insets expansion) {
            this.coordinateSystem = coordinateSystem;
            this.offset = offset;
            this.expansion = expansion;
        }

        public CoordinateSystem getCoordinateSystem() {
            return coordinateSystem;
        }

        public Insets getOffset() {
            return offset;
        }

        public Insets getExpansion() {
            return expansion;
        }

        public boolean isCenterBased() {
            return coordinateSystem == CoordinateSystem.CENTER_BASED;
        }

        public boolean isTopLeftBased() {
            return coordinateSystem == CoordinateSystem.TOP_LEFT_BASED;
        }

        public boolean isBottomCenterBased() {
            return coordinateSystem == CoordinateSystem.BOTTOM_CENTER_BASED;
        }
    }
}
