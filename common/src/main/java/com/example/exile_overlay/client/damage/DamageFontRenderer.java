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
        renderText(poseStack, text, x, y, color, bufferSource, packedLight, -1.0f);
    }
    
    public static void renderText(PoseStack poseStack, String text, float x, float y, 
                                  int color, MultiBufferSource bufferSource, int packedLight, float scale) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        FontPreset preset = config.getFontPreset();
        
        if (preset == FontPreset.MINECRAFT) {
            renderWithMinecraftFont(poseStack, text, x, y, color, bufferSource, packedLight);
        } else if (preset == FontPreset.LINESEED) {
            renderWithDamageFont(poseStack, text, x, y, color, bufferSource, packedLight);
        } else if (preset.isCustomFont()) {
            renderWithCustomFont(poseStack, text, x, y, color, bufferSource, packedLight, scale, config);
        } else {
            renderWithDamageFont(poseStack, text, x, y, color, bufferSource, packedLight);
        }
    }
    
    private static void renderWithMinecraftFont(PoseStack poseStack, String text, float x, float y,
                                                 int color, MultiBufferSource bufferSource, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        mc.font.drawInBatch(text, x, y, color, false,
            poseStack.last().pose(), bufferSource,
            Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    }
    
    private static void renderWithDamageFont(PoseStack poseStack, String text, float x, float y,
                                              int color, MultiBufferSource bufferSource, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        Component component = Component.literal(text)
            .withStyle(Style.EMPTY.withFont(DAMAGE_FONT));
        mc.font.drawInBatch(component, x, y, color, false,
            poseStack.last().pose(), bufferSource,
            Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    }
    
    private static void renderWithCustomFont(PoseStack poseStack, String text, float x, float y,
                                             int color, MultiBufferSource bufferSource, int packedLight,
                                             float scale, DamagePopupConfig config) {
        if (!fontInitialized) {
            initializeCustomFont(config);
        }
        
        CustomDamageFontRenderer customRenderer = CustomDamageFontRenderer.getInstance();
        if (customRenderer.isFontLoaded()) {
            float actualScale = scale > 0 ? scale : config.getBaseScale();
            customRenderer.renderText(poseStack, text, x, y, color, bufferSource, packedLight, actualScale);
        } else {
            renderWithDamageFont(poseStack, text, x, y, color, bufferSource, packedLight);
        }
    }
    
    private static void initializeCustomFont(DamagePopupConfig config) {
        fontInitialized = true;
        FontPreset preset = config.getFontPreset();
        
        if (!preset.isCustomFont() || preset.isJsonFont()) {
            return;
        }
        
        String fontPath = preset.getResourcePath();
        boolean loaded = CustomDamageFontRenderer.getInstance().loadFontFromResource(
            fontPath, config.getCustomFontSize()
        );
        
        if (!loaded) {
            LOGGER.warn("Failed to load custom font: {}, using default font", fontPath);
        }
    }
    
    public static void reloadCustomFont() {
        fontInitialized = false;
        CustomDamageFontRenderer.getInstance().clearCache();
        LOGGER.info("Custom font cache cleared, will reload on next render");
    }
}