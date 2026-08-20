package com.example.exile_overlay.api;

import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD要素の統一レンダリングインターフェース。
 * レイヤー・優先度順にパイプライン実行される。
 * isVisible()で事前に描画対象か判定し、execute()内では描画のみ実行。
 */
public interface IRenderCommand {

    String getId();

    void render(GuiGraphics graphics, RenderContext ctx);

    default void execute(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx);
    }

    default boolean isVisible(RenderContext ctx) {
        return getPosition().isVisible();
    }

    default RenderLayer getLayer() {
        return RenderLayer.FILL;
    }

    default int getPriority() {
        return 100;
    }

    default String getConfigKey() {
        return getId();
    }

    int getWidth();

    int getHeight();

    default int getConfigWidth() {
        return getWidth();
    }

    default int getConfigHeight() {
        return getHeight();
    }

    default boolean isDraggable() {
        return false;
    }

    default HudPosition getPosition() {
        return HudPositionManager.getInstance().getPosition(getConfigKey());
    }

    default float getScale() {
        return getPosition().getScale();
    }

    default int[] resolvePosition(int screenWidth, int screenHeight) {
        return getPosition().resolve(screenWidth, screenHeight);
    }

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

    default int[] getDragPreviewPosition(int screenWidth, int screenHeight, int dragOffsetX, int dragOffsetY) {
        int[] basePos = resolvePosition(screenWidth, screenHeight);
        return new int[]{basePos[0] + dragOffsetX, basePos[1] + dragOffsetY};
    }

    default HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
            CoordinateSystem.CENTER_BASED,
            new Insets(0, 0, 0, 0),
            new Insets(0, 0, 0, 0)
        );
    }

    default int[] getFallbackSize() {
        return new int[]{80, 40};
    }

    enum CoordinateSystem {
        CENTER_BASED,
        TOP_LEFT_BASED,
        BOTTOM_CENTER_BASED
    }

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
