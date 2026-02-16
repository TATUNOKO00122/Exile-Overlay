package com.example.exile_overlay.client.damage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DamageFontRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageFont");
    private static final ResourceLocation DAMAGE_FONT = 
        ResourceLocation.tryParse("exile_overlay:damage_font");
    
    private static boolean fontInitialized = false;

    public static void renderText(PoseStack poseStack, String text, float x, float y, 
                                  int color, MultiBufferSource bufferSource, int packedLight) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        
        // カスタムフォントが有効で、フォントが読み込まれている場合
        if (config.isUseCustomFont()) {
            if (!fontInitialized) {
                initializeCustomFont(config);
            }
            
            CustomDamageFontRenderer customRenderer = CustomDamageFontRenderer.getInstance();
            if (customRenderer.isFontLoaded()) {
                customRenderer.renderText(poseStack, text, x, y, color, bufferSource, packedLight);
                return;
            }
        }
        
        // デフォルトフォントを使用
        renderWithDefaultFont(poseStack, text, x, y, color, bufferSource, packedLight);
    }
    
    private static void initializeCustomFont(DamagePopupConfig config) {
        fontInitialized = true;
        String fontPath = config.getCustomFontPath();
        
        if (fontPath != null && !fontPath.isEmpty()) {
            boolean loaded = CustomDamageFontRenderer.getInstance().loadFont(
                fontPath, config.getCustomFontSize()
            );
            if (!loaded) {
                LOGGER.warn("Failed to load custom font: {}, using default font", fontPath);
            }
        } else {
            LOGGER.warn("Custom font path is not set, using default font");
        }
    }
    
    private static void renderWithDefaultFont(PoseStack poseStack, String text, float x, float y, 
                                              int color, MultiBufferSource bufferSource, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        
        Component component = Component.literal(text)
            .withStyle(Style.EMPTY.withFont(DAMAGE_FONT));

        mc.font.drawInBatch(component, x, y, color, false,
            poseStack.last().pose(), bufferSource,
            Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    }
    
    /**
     * カスタムフォントを再読み込み
     */
    public static void reloadCustomFont() {
        fontInitialized = false;
        CustomDamageFontRenderer.getInstance().clearCache();
        LOGGER.info("Custom font cache cleared, will reload on next render");
    }
}
