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

    public static void renderText(PoseStack poseStack, String text, float x, float y,
                                  int color, MultiBufferSource bufferSource, int packedLight) {
        FontPreset preset = DamagePopupConfig.getInstance().getFontPreset();
        ResourceLocation fontLoc = preset.getFontLocation();
        Style fontStyle = fontLoc != null ? Style.EMPTY.withFont(fontLoc) : Style.EMPTY;

        Minecraft mc = Minecraft.getInstance();
        mc.font.drawInBatch(Component.literal(text).withStyle(fontStyle),
            x, y, color, false,
            poseStack.last().pose(), bufferSource,
            Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    }
}
