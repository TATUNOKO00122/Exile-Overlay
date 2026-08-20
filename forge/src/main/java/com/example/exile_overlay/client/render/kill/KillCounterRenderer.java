package com.example.exile_overlay.client.render.kill;

/*
import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/ **
 * キルカウンター HUD レンダラー
 *
 * - 左側に赤いドクロマークアイコンを表示
 * - 右側に高解像度TTFフォントによる赤色キルカウント数値を美麗に描画
 * - キル発生時の滑らかな拡大縮小（ポップ）モーション
 * - キル数0の時は非表示（HUD編集画面ではプレビュー表示）
 * - ドラッグ移動・スケール調整対応
 * /
public class KillCounterRenderer implements IRenderCommand {

    private static final String COMMAND_ID = "kill_counter";
    private static final ResourceLocation SKULL_TEXTURE =
            new ResourceLocation("exile_overlay", "textures/gui/kill_count_icon.png");

    private static final int SKULL_SIZE = 16;
    private static final int GAP = 1;
    private static final int PREFIX_GAP = 2;
    private static final int ESTIMATED_TEXT_WIDTH = 36;
    private static final int PRIORITY = 80;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @Override
    public String getId() {
        return COMMAND_ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.OVERLAY;
    }

    @Override
    public String getConfigKey() {
        return COMMAND_ID;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public int getWidth() {
        return SKULL_SIZE + GAP + ESTIMATED_TEXT_WIDTH;
    }

    @Override
    public int getHeight() {
        return 18;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DraggableHudConfigScreen) {
            return getPosition().isVisible();
        }
        // キル数が0の場合は表示しない
        return getPosition().isVisible() && KillCountManager.getInstance().getKillCount() > 0;
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.TOP_LEFT_BASED,
                new Insets(0, 0, 0, 0),
                new Insets(0, 0, 0, 0)
        );
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int killCount = KillCountManager.getInstance().getKillCount();
        boolean isEditing = mc.screen instanceof DraggableHudConfigScreen;

        // キル数が0で編集画面でもない場合は描画しない
        if (killCount <= 0 && !isEditing) {
            return;
        }

        // HUD設定画面（ドラッグ中）かつキル数が0の場合はプレビュー用の数値を表示
        if (isEditing && killCount == 0) {
            killCount = 38;
        }

        HudPosition position = getPosition();
        int[] pos = position.resolve(ctx.getScreenWidth(), ctx.getScreenHeight());
        float baseScale = getScale();
        float popMultiplier = KillCountManager.getInstance().getScaleMultiplier();

        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().translate(pos[0], pos[1], 0);
        graphics.pose().scale(baseScale, baseScale, 1.0f);

        // ドクロアイコン描画 (左側)
        int skullY = (getHeight() - SKULL_SIZE) / 2;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(SKULL_TEXTURE, 0, skullY, 0, 0, SKULL_SIZE, SKULL_SIZE, SKULL_SIZE, SKULL_SIZE);

        // キルカウントのプレフィックスと数値
        String prefix = "x";
        String numText = String.valueOf(killCount);

        int prefixWidth = mc.font.width(prefix);
        int numWidth = mc.font.width(numText);
        int textHeight = mc.font.lineHeight;

        int textX = SKULL_SIZE + GAP;
        int textY = (getHeight() - textHeight) / 2 + 1;

        // プレフィックス「x 」はアニメーションさせずに描画
        graphics.drawString(mc.font, prefix, textX, textY, TEXT_COLOR, true);

        // 数字部分の開始X座標
        int numX = textX + prefixWidth + PREFIX_GAP;

        // 数字部分のみポップアニメーション（キル発生時に文字の中心を原点として拡大縮小）
        graphics.pose().pushPose();
        if (popMultiplier > 1.001f) {
            float centerX = numX + numWidth / 2.0f;
            float centerY = textY + textHeight / 2.0f;
            graphics.pose().translate(centerX, centerY, 0);
            graphics.pose().scale(popMultiplier, popMultiplier, 1.0f);
            graphics.pose().translate(-centerX, -centerY, 0);
        }

        // 数字部分の描画（ドロップシャドウ付き）
        graphics.drawString(mc.font, numText, numX, textY, TEXT_COLOR, true);

        graphics.pose().popPose();

        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }
}
*/
