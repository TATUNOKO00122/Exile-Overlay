package com.example.exile_overlay.client.damage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public class DamageFontRenderer {
    private static final ResourceLocation DAMAGE_FONT = java.util.Objects
            .requireNonNull(ResourceLocation.tryParse("exile_overlay:damage_font"));

    public static void renderText(PoseStack poseStack, String text, float x, float y, int color,
            MultiBufferSource bufferSource, int packedLight) {
        Minecraft mc = Minecraft.getInstance();

        Component component = Component.literal(text)
                .withStyle(Style.EMPTY.withFont(DAMAGE_FONT));

        mc.font.drawInBatch(component, x, y, color, false,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    }
}
