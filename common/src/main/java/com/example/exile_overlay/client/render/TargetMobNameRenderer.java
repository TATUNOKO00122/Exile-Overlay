package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ターゲットMOB名表示レンダラー
 * 
 * プレイヤーが見ているMOBの名前をHUDに表示する
 * IHudRendererを実装して位置設定に対応
 */
public class TargetMobNameRenderer implements IRenderCommand, IHudRenderer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetMobNameRenderer.class);
    private static final String COMMAND_ID = "target_mob_name";
    private static final int PRIORITY = 80;
    
    // 最大検出距離
    private static final double MAX_DISTANCE = 64.0;
    
    // デフォルトサイズ
    private static final int DEFAULT_BG_WIDTH = 120;
    private static final int DEFAULT_BG_HEIGHT = 17;
    private static final int PADDING = 4;
    
    public TargetMobNameRenderer() {
    }
    
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
    public boolean isVisible(RenderContext ctx) {
        if (!IHudRenderer.super.isVisible(ctx)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }
    
    @Override
    public void execute(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx);
    }
    
    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        // ターゲットエンティティを取得
        LivingEntity target = getTargetEntity(mc.player, MAX_DISTANCE);
        if (target == null) {
            return;
        }
        
        // 表示名を取得
        String name = target.getDisplayName().getString();
        if (name.isEmpty()) {
            return;
        }
        
        // 画面サイズを取得
        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();
        
        // 位置とスケールを取得
        HudPosition position = getPosition();
        int[] pos = position.resolve(screenWidth, screenHeight);
        float scale = getScale();
        
        // テキストサイズを計算
        int textWidth = mc.font.width(name);
        int bgWidth = Math.max(DEFAULT_BG_WIDTH, textWidth + PADDING * 2);
        int bgHeight = DEFAULT_BG_HEIGHT;
        
        // 位置を計算（中心基準）
        int x = pos[0] - (int) ((bgWidth * scale) / 2);
        int y = pos[1] - (int) ((bgHeight * scale) / 2);
        
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            
            // 背景を描画
            int bgColor = 0x80000000; // 50%透明な黒
            graphics.fill(0, 0, bgWidth, bgHeight, bgColor);
            
            // テキストを描画（中央揃え）
            int textX = (bgWidth - textWidth) / 2;
            int textY = PADDING;
            graphics.drawString(mc.font, name, textX, textY, 0xFFFFFFFF, true);
            
        } finally {
            graphics.pose().popPose();
        }
    }
    
    /**
     * プレイヤーが見ているエンティティを取得
     *
     * @param player プレイヤー
     * @param maxDistance 最大距離
     * @return ターゲットのLivingEntity、なければnull
     */
    private static LivingEntity getTargetEntity(Player player, double maxDistance) {
        try {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

            // エンティティ検索用のAABBを作成
            AABB searchBox = player.getBoundingBox()
                    .expandTowards(lookVec.scale(maxDistance))
                    .inflate(1.0);

            // エンティティへのレイキャスト
            EntityHitResult result = ProjectileUtil.getEntityHitResult(
                    player,
                    eyePos,
                    endPos,
                    searchBox,
                    entity -> entity instanceof LivingEntity
                            && !entity.isSpectator()
                            && entity.isPickable()
                            && !(entity instanceof Player),
                    maxDistance * maxDistance);

            if (result != null && result.getEntity() instanceof LivingEntity living) {
                return living;
            }

        } catch (Exception e) {
            LOGGER.error("Failed to get target entity", e);
        }

        return null;
    }
    
    // IHudRenderer implementation
    
    @Override
    public String getConfigKey() {
        return "target_mob_name";
    }
    
    @Override
    public int getWidth() {
        return DEFAULT_BG_WIDTH;
    }
    
    @Override
    public int getHeight() {
        return DEFAULT_BG_HEIGHT;
    }
    
    @Override
    public boolean isDraggable() {
        return true;
    }
    
    @Override
    public HudRenderMetadata getRenderMetadata() {
        // 中心基準
        return new HudRenderMetadata(
            CoordinateSystem.CENTER_BASED,
            new Insets(0, 0, 0, 0),
            new Insets(0, 0, 0, 0)
        );
    }
}
