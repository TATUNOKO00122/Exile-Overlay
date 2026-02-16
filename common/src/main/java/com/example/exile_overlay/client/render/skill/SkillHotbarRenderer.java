package com.example.exile_overlay.client.render.skill;

import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.util.MineAndSlashHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillHotbarRenderer implements IRenderCommand, IHudRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillHotbarRenderer.class);

    public static final int SLOT_COUNT = 8;
    public static final int SLOT_SIZE = 21;
    public static final int SLOT_SPACING = 1;
    public static final int ICON_SIZE = 15;
    public static final int ICON_OFFSET = 3;

    private static final ResourceLocation SLOT_FRAME_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/skill_slot_frame.png");

    private static final int COOLDOWN_OVERLAY_COLOR = 0xAA000000;

    @Override
    public String getId() {
        return "skill_hotbar";
    }

    @Override
    public String getConfigKey() {
        return "skill_hotbar";
    }

    @Override
    public void execute(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx);
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        if (!MineAndSlashHelper.isLoaded()) {
            return false;
        }
        return IHudRenderer.super.isVisible(ctx);
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = ctx.getMinecraft();
        Player player = ctx.getPlayer();
        if (player == null || mc.options.hideGui) {
            return;
        }

        if (!MineAndSlashHelper.isLoaded()) {
            return;
        }

        HudPosition position = getPosition();
        boolean horizontal = position.isHorizontal();

        int totalWidth = getConfigWidth();
        int totalHeight = getConfigHeight();

        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();
        int[] pos = position.resolve(screenWidth, screenHeight);
        int x = pos[0];
        int y = pos[1];
        float scale = position.getScale();

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        // 中心基準：設定画面と同じ計算方法を使用
        // 設定画面：left = x - (width * scale) / 2
        // レンダリング：translate(x, y) 後、スケール後座標系で描画
        // スケール後座標系では1単位 = scaleピクセルなので、
        // スケール後座標系で (width * scale / 2) / scale = width / 2 移動すれば良い
        int offsetX = -totalWidth / 2;
        int offsetY = -totalHeight / 2;

        int visibleIndex = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ResourceLocation icon = MineAndSlashHelper.getSpellIcon(player, slot);

            if (icon == null) {
                continue;
            }

            int slotX, slotY;
            if (horizontal) {
                slotX = offsetX + visibleIndex * (SLOT_SIZE + SLOT_SPACING);
                slotY = offsetY;
            } else {
                slotX = offsetX;
                slotY = offsetY + visibleIndex * (SLOT_SIZE + SLOT_SPACING);
            }
            visibleIndex++;

            int iconX = slotX + ICON_OFFSET;
            int iconY = slotY + ICON_OFFSET;

            RenderSystem.enableBlend();
            graphics.blit(icon, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            float cdPercent = MineAndSlashHelper.getSpellCooldownPercent(player, slot);
            if (cdPercent > 0) {
                drawCooldownOverlay(graphics, iconX, iconY, cdPercent);

                int seconds = MineAndSlashHelper.getSpellCooldownSeconds(player, slot);
                if (seconds > 0) {
                    String text = String.valueOf(seconds);
                    int textX = iconX + ICON_SIZE / 2 - mc.font.width(text) / 2;
                    int textY = iconY + ICON_SIZE / 2 - 4;
                    graphics.drawString(mc.font, text, textX + 1, textY + 1, 0xFF000000, false);
                    graphics.drawString(mc.font, text, textX, textY, 0xFFFFFF00, false);
                }
            }

            RenderSystem.enableBlend();
            graphics.blit(SLOT_FRAME_TEXTURE, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

            int manaCost = MineAndSlashHelper.getSpellManaCost(player, slot);
            if (manaCost > 0) {
                graphics.pose().pushPose();
                float manaTextScale = 0.5f;
                graphics.pose().scale(manaTextScale, manaTextScale, 1.0f);

                float scaledSlotX = slotX / manaTextScale;
                float scaledSlotY = slotY / manaTextScale;

                String manaText = String.valueOf(manaCost);
                int textWidth = mc.font.width(manaText);
                graphics.fill((int) scaledSlotX + 2, (int) scaledSlotY + 2,
                        (int) scaledSlotX + textWidth + 4, (int) scaledSlotY + 12, 0xAA000066);
                graphics.drawString(mc.font, manaText, (int) scaledSlotX + 3, (int) scaledSlotY + 3, 0xFF00CCFF, false);

                graphics.pose().popPose();
            }

            String keyText = MineAndSlashHelper.getSpellKeyText(slot).toUpperCase();
            keyText = keyText.replace("LEFT SHIFT", "S").replace("RIGHT SHIFT", "S").replace("SHIFT", "S");
            keyText = keyText.replace("LEFT CONTROL", "C").replace("RIGHT CONTROL", "C").replace("CONTROL", "C");
            keyText = keyText.replace("LEFT ALT", "A").replace("RIGHT ALT", "A").replace("ALT", "A");
            keyText = keyText.replace(" + ", "+");
            keyText = keyText.replace(" ", "");

            graphics.pose().pushPose();
            float textScale = 0.4f;
            graphics.pose().scale(textScale, textScale, 1.0f);

            float scaledX = slotX / textScale;
            float scaledY = slotY / textScale;
            float scaledSlotSize = SLOT_SIZE / textScale;

            int textWidth = mc.font.width(keyText);
            float keyX = scaledX + scaledSlotSize - textWidth - 7;
            float keyY = scaledY + scaledSlotSize - 14;

            graphics.drawString(mc.font, keyText, (int) keyX, (int) keyY, 0xFFFFFFFF, false);

            graphics.pose().popPose();
        }

        graphics.pose().popPose();
    }

    private void drawCooldownOverlay(GuiGraphics graphics, int x, int y, float percent) {
        int overlayHeight = (int) (ICON_SIZE * percent);
        if (overlayHeight > 0) {
            graphics.fill(x, y, x + ICON_SIZE, y + overlayHeight, COOLDOWN_OVERLAY_COLOR);
            graphics.fill(x, y + overlayHeight - 1, x + ICON_SIZE, y + overlayHeight, 0xFF00FF00);
        }
    }

    @Override
    public int getWidth() {
        HudPosition position = getPosition();
        if (position.isHorizontal()) {
            return (SLOT_SIZE * SLOT_COUNT) + (SLOT_SPACING * (SLOT_COUNT - 1));
        } else {
            return SLOT_SIZE;
        }
    }

    @Override
    public int getHeight() {
        HudPosition position = getPosition();
        if (position.isHorizontal()) {
            return SLOT_SIZE;
        } else {
            return (SLOT_SIZE * SLOT_COUNT) + (SLOT_SPACING * (SLOT_COUNT - 1));
        }
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    /**
     * 実際に表示されているスキルの数をカウント
     */
    private int countVisibleSkills(Player player) {
        if (player == null || !MineAndSlashHelper.isLoaded()) {
            return 0;
        }

        int count = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ResourceLocation icon = MineAndSlashHelper.getSpellIcon(player, slot);
            if (icon != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 設定画面用の幅を取得
     * 表示されているスキル数に応じた動的サイズを返す
     */
    @Override
    public int getConfigWidth() {
        Minecraft mc = Minecraft.getInstance();
        int visibleCount = countVisibleSkills(mc.player);
        int count = Math.max(visibleCount, 1);

        HudPosition position = getPosition();
        if (position.isHorizontal()) {
            return (SLOT_SIZE * count) + (SLOT_SPACING * (count - 1));
        } else {
            return SLOT_SIZE;
        }
    }

    /**
     * 設定画面用の高さを取得
     * 表示されているスキル数に応じた動的サイズを返す
     */
    @Override
    public int getConfigHeight() {
        Minecraft mc = Minecraft.getInstance();
        int visibleCount = countVisibleSkills(mc.player);
        int count = Math.max(visibleCount, 1);

        HudPosition position = getPosition();
        if (position.isHorizontal()) {
            return SLOT_SIZE;
        } else {
            return (SLOT_SIZE * count) + (SLOT_SPACING * (count - 1));
        }
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.CENTER_BASED,
                new Insets(0, 0, 0, 0),
                new Insets(0, 0, 0, 0)
        );
    }
}
