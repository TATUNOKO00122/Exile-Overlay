package com.example.exile_overlay.client.render.orb;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * オーブ描画を担当するクラス
 */
public class OrbRenderer {
    
    private static final ResourceLocation REFLECTION_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/orb_reflection.png");
    
    /**
     * オーブを描画する
     */
    public static void render(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc) {
        if (!config.isVisible(player)) {
            return;
        }
        
        float current = config.getDataProvider().getCurrentValue(player);
        float max = config.getDataProvider().getMaxValue(player);
        float percent = max > 0 ? Math.min(current / max, 1.0f) : 0;
        
        int orbX = config.getCenterX();
        int orbY = config.getCenterY();
        int orbSize = config.getSize();
        
        // メインのオーブ描画
        drawCircularFill(graphics, orbX, orbY, orbSize, percent, config.getColor());
        
        // オーバーレイ描画（Magic Shieldなど）
        if (config.getOverlayColor() != 0x00FFFFFF) {
            renderOverlayOrb(graphics, config, player);
        }
        
        // 反射テクスチャ
        if (config.shouldShowReflection()) {
            RenderSystem.enableBlend();
            graphics.blit(REFLECTION_TEXTURE, orbX, orbY, 0, 0, 0, orbSize, orbSize, orbSize, orbSize);
        }
        
        // 値の表示
        if (config.getDataProvider().shouldShowValue()) {
            String text = (int) current + " / " + (int) max;
            renderCenteredScaledText(graphics, mc, text, orbX + orbSize / 2, orbY + orbSize / 2, 0.8f, 0xFFFFFFFF);
        }
    }
    
    /**
     * オーバーレイオーブを描画（Magic Shieldなど）
     */
    private static void renderOverlayOrb(GuiGraphics graphics, OrbConfig config, Player player) {
        // Magic Shieldの場合は専用のデータプロバイダーから取得
        if ("magic_shield".equals(config.getId())) {
            return; // メイン処理で既に描画済み
        }
        
        // HPオーブの場合、Magic Shieldをオーバーレイとして描画
        if ("health".equals(config.getId())) {
            float currentEs = OrbDataProviders.MAGIC_SHIELD.getCurrentValue(player);
            float maxEs = OrbDataProviders.MAGIC_SHIELD.getMaxValue(player);
            float esPercent = maxEs > 0 ? Math.min(currentEs / maxEs, 1.0f) : 0;
            
            if (maxEs > 0 && esPercent > 0) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
                );
                drawCircularFill(graphics, config.getCenterX(), config.getCenterY(), config.getSize(), esPercent, config.getOverlayColor());
                RenderSystem.defaultBlendFunc();
            }
        }
    }
    
    /**
     * 円形の塗りつぶしを描画
     */
    public static void drawCircularFill(GuiGraphics graphics, int x, int y, int size, float fillPercent, int color) {
        if (fillPercent <= 0) return;
        
        float radius = size / 2.0f;
        float centerX = x + radius;
        float centerY = y + radius;
        float liquidTopY = centerY + radius - (fillPercent * size);
        int startY = Math.max((int) Math.floor(liquidTopY), y);
        int endY = Math.min((int) Math.ceil(centerY + radius), y + size);
        
        for (int curY = startY; curY < endY; curY++) {
            float dy = (curY + 0.5f) - centerY;
            float radSq = radius * radius;
            float distSq = dy * dy;
            if (distSq >= radSq) continue;
            float halfWidth = (float) Math.sqrt(radSq - distSq);
            graphics.fill(Math.round(centerX - halfWidth), curY, Math.round(centerX + halfWidth), curY + 1, color);
        }
    }
    
    /**
     * 中央揃えのテキストを描画
     */
    public static void renderCenteredScaledText(GuiGraphics graphics, Minecraft mc, String text, 
                                                  float centerX, float centerY, float scale, int color) {
        int textWidth = mc.font.width(text);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(mc.font, text, -textWidth / 2, -mc.font.lineHeight / 2, color, true);
        graphics.pose().popPose();
    }
}
