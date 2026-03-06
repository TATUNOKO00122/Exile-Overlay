package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArmorDurabilityRenderer implements IRenderCommand, IHudRenderer {

    private static final String COMMAND_ID = "armor_durability";
    private static final int PRIORITY = 90;

    private static final int ITEM_SIZE = 16;
    private static final int GAP = 4;
    private static final int LINE_HEIGHT = ITEM_SIZE + GAP;
    private static final int TEXT_OFFSET = ITEM_SIZE + 4;
    private static final int ESTIMATED_TEXT_WIDTH = 62;
    private static final int ESTIMATED_WIDTH = TEXT_OFFSET + ESTIMATED_TEXT_WIDTH;
    private static final int MAX_ITEMS = 5;

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
        return IHudRenderer.super.isVisible(ctx);
    }

    @Override
    public void execute(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx);
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        HudPosition position = getPosition();
        int[] pos = position.resolve(ctx.getScreenWidth(), ctx.getScreenHeight());
        float scale = getScale();

        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().translate(pos[0], pos[1], 0);
        graphics.pose().scale(scale, scale, 1.0f);

        EquipmentSlot[] slots = {
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

        // 表示するアイテムを収集
        List<ItemStack> itemsToRender = new ArrayList<>();
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                itemsToRender.add(stack);
            }
        }

        // 下から上に描画
        // BOTTOM_CENTER_BASED: Y=0が底辺なので、負の方向へ向かって描画
        int totalHeight = itemsToRender.size() * LINE_HEIGHT;
        int currentY = -totalHeight; // 最も上のアイテムの位置
        int centerX = 0; // X=0が中心

        for (ItemStack stack : itemsToRender) {
            renderItem(graphics, mc, stack, currentY, centerX);
            currentY += LINE_HEIGHT; // 下へ向かって進む（Y値は増えて0に近づく）
        }

        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private void renderItem(GuiGraphics graphics, Minecraft mc, ItemStack stack, int y, int centerX) {
        // アイコンを描画（中心から左に寄せる）
        int leftX = centerX - ESTIMATED_WIDTH / 2;
        graphics.renderItem(stack, leftX, y);
        // 耐久度Barを描画しない（renderItemDecorationsを呼ばない）

        if (stack.isDamageableItem()) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamageValue();
            int remaining = maxDamage - currentDamage;
            float durabilityPercent = (float) remaining / maxDamage;
            int color = getDurabilityColor(durabilityPercent);

            String text;
            if (EquipmentDisplayConfig.getInstance().isUsePercentage()) {
                text = String.format("%.0f%%", durabilityPercent * 100);
            } else {
                text = String.format("%d/%d", remaining, maxDamage);
            }

            boolean shadow = EquipmentDisplayConfig.getInstance().isEnableShadow();
            graphics.drawString(mc.font, text, leftX + TEXT_OFFSET, y + 4, color, shadow);
        }
    }

    private int getDurabilityColor(float durabilityPercent) {
        if (durabilityPercent < 0.2f) {
            return 0xFF5555;
        } else if (durabilityPercent < 0.5f) {
            return 0xFFFF55;
        } else {
            return 0x55FF55;
        }
    }

    @Override
    public String getConfigKey() {
        return "armor_durability";
    }

    @Override
    public int getWidth() {
        return ESTIMATED_WIDTH;
    }

    @Override
    public int getHeight() {
        return MAX_ITEMS * LINE_HEIGHT;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        // 下から上に増える描画のため、Yは底辺基準
        return new HudRenderMetadata(
            CoordinateSystem.BOTTOM_CENTER_BASED,
            new Insets(0, 0, 0, 0),
            new Insets(0, 0, 0, 0)
        );
    }
}
