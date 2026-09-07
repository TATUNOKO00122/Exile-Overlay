package com.example.exile_overlay.client.render.vanilla;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.compat.AppleSkinCompat;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaFoodRenderer implements IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaFoodRenderer.class);
    public static final String CONFIG_KEY = "vanilla_food";

    private static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int ICON_SIZE = 9;
    private static final int MAX_FOOD = 10;
    private static final int SPACING = 8;
    private static final HudPositionManager POSITION_MANAGER = HudPositionManager.getInstance();

    private static boolean positionDirty = true;

    static {
        POSITION_MANAGER.addListener(CONFIG_KEY, (key, newPosition) -> {
            positionDirty = true;
        });
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (!isVisible(ctx)) {
            return;
        }

        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();
        HudPosition position = POSITION_MANAGER.getPosition(CONFIG_KEY);
        int[] pos = position.resolve(screenWidth, screenHeight);
        float scale = position.getScale();

        renderFoodBar(graphics, mc.player, pos[0], pos[1], scale);
    }

    private void renderFoodBar(GuiGraphics graphics, Player player, int x, int y, float scale) {
        FoodData foodData = player.getFoodData();
        int foodLevel = foodData.getFoodLevel();
        float saturationLevel = foodData.getSaturationLevel();
        float exhaustionLevel = foodData.getExhaustionLevel();

        boolean isAppleSkin = AppleSkinCompat.isLoaded();
        if (isAppleSkin) {
            AppleSkinCompat.tick();
        }

        RenderSystem.enableBlend();

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        // 1. 隠し疲労度 (Exhaustion Underlay) の描画
        if (isAppleSkin) {
            renderExhaustionUnderlay(graphics, exhaustionLevel);
        }

        // 2. バニラフードバー (背景 ＆ 現在の満腹度) の描画
        for (int i = 0; i < MAX_FOOD; i++) {
            int iconX = (9 - i) * SPACING;
            int iconY = 0;

            // 背景 (空アイコン) - U=16
            graphics.blit(GUI_ICONS, iconX, iconY, 16, 27, ICON_SIZE, ICON_SIZE);

            // 満腹/半分アイコンを上に重ねる
            if (i * 2 + 1 < foodLevel) {
                // 満腹 - U=52
                graphics.blit(GUI_ICONS, iconX, iconY, 52, 27, ICON_SIZE, ICON_SIZE);
            } else if (i * 2 + 1 == foodLevel) {
                // 半分 - U=61
                graphics.blit(GUI_ICONS, iconX, iconY, 61, 27, ICON_SIZE, ICON_SIZE);
            }
        }

        // 3. 食べ物持参時の回復量プレビュー ＆ 隠し満腹度オーバーレイ (AppleSkin連携)
        if (isAppleSkin) {
            renderAppleSkinOverlays(graphics, player, foodLevel, saturationLevel);
        }

        graphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderExhaustionUnderlay(GuiGraphics graphics, float exhaustionLevel) {
        float maxExhaustion = 4.0f;
        float ratio = Math.min(1.0f, Math.max(0.0f, exhaustionLevel / maxExhaustion));
        int totalWidth = (MAX_FOOD - 1) * SPACING + ICON_SIZE; // 81px
        int width = (int) (ratio * totalWidth);

        if (width > 0) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.75F);
            graphics.blit(AppleSkinCompat.APPLESKIN_ICONS, totalWidth - width, 0, 81 - width, 18, width, ICON_SIZE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void renderAppleSkinOverlays(GuiGraphics graphics, Player player, int foodLevel, float saturationLevel) {
        // 現在の隠し満腹度オーバーレイ描画
        renderSaturationOverlay(graphics, saturationLevel, 0f, 1.0f);

        // 食べ物アイテムの回復量プレビュー描画
        net.minecraft.world.item.ItemStack heldItem = AppleSkinCompat.getHeldFoodItem(player);
        if (!heldItem.isEmpty()) {
            AppleSkinCompat.FoodValues foodValues = AppleSkinCompat.getFoodValues(heldItem, player);
            int hungerRestored = foodValues.hunger();
            float saturationIncrement = foodValues.getSaturationIncrement();
            float flashAlpha = AppleSkinCompat.getFlashAlpha();
            boolean isRotten = AppleSkinCompat.isRotten(heldItem, player);

            if (hungerRestored > 0 && flashAlpha > 0f) {
                renderHungerRestoredPreview(graphics, foodLevel, hungerRestored, flashAlpha, isRotten);

                int newFoodValue = Math.min(20, foodLevel + hungerRestored);
                float newSaturationValue = saturationLevel + saturationIncrement;
                float saturationGained = newSaturationValue > newFoodValue ? Math.max(0, newFoodValue - saturationLevel) : saturationIncrement;

                if (saturationGained > 0) {
                    renderSaturationOverlay(graphics, saturationLevel, saturationGained, flashAlpha);
                }
            }
        }
    }

    private void renderHungerRestoredPreview(GuiGraphics graphics, int foodLevel, int hungerRestored, float alpha, boolean useRotten) {
        int modifiedFood = Math.max(0, Math.min(20, foodLevel + hungerRestored));
        int startFoodBars = Math.max(0, foodLevel / 2);
        int endFoodBars = (int) Math.ceil(modifiedFood / 2.0F);

        int iconStartOffset = 16;

        for (int i = startFoodBars; i < endFoodBars; i++) {
            int iconX = (9 - i) * SPACING;
            int iconY = 0;

            int v = 3 * ICON_SIZE; // 27
            int u = iconStartOffset + 4 * ICON_SIZE; // 52
            int ub = iconStartOffset + 1 * ICON_SIZE; // 25

            if (useRotten) {
                u += 4 * ICON_SIZE;
                ub += 12 * ICON_SIZE;
            }

            if (i * 2 + 1 == modifiedFood) {
                u += 1 * ICON_SIZE;
            }

            // 薄い背景
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * 0.25F);
            graphics.blit(GUI_ICONS, iconX, iconY, ub, v, ICON_SIZE, ICON_SIZE);

            // 点滅回復アイコン
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            graphics.blit(GUI_ICONS, iconX, iconY, u, v, ICON_SIZE, ICON_SIZE);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderSaturationOverlay(GuiGraphics graphics, float saturationLevel, float saturationGained, float alpha) {
        float modifiedSaturation = Math.max(0, Math.min(saturationLevel + saturationGained, 20));
        if (modifiedSaturation <= 0) return;

        int startSaturationBar = 0;
        int endSaturationBar = (int) Math.ceil(modifiedSaturation / 2.0F);

        if (saturationGained > 0) {
            startSaturationBar = (int) Math.max(saturationLevel / 2.0F, 0);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        for (int i = startSaturationBar; i < endSaturationBar; i++) {
            int iconX = (9 - i) * SPACING;
            int iconY = 0;

            float effectiveSaturation = (modifiedSaturation / 2.0F) - i;
            int u = 0;
            if (effectiveSaturation >= 1.0f) {
                u = 3 * ICON_SIZE; // 27
            } else if (effectiveSaturation > 0.5f) {
                u = 2 * ICON_SIZE; // 18
            } else if (effectiveSaturation > 0.25f) {
                u = 1 * ICON_SIZE; // 9
            }

            graphics.blit(AppleSkinCompat.APPLESKIN_ICONS, iconX, iconY, u, 0, ICON_SIZE, ICON_SIZE);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public String getId() {
        return CONFIG_KEY;
    }

    @Override
    public String getConfigKey() {
        return CONFIG_KEY;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        return IRenderCommand.super.isVisible(ctx);
    }

    @Override
    public int getWidth() {
        return (MAX_FOOD - 1) * SPACING + ICON_SIZE;
    }

    @Override
    public int getHeight() {
        return ICON_SIZE;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.FILL;
    }

    @Override
    public int getPriority() {
        return IRenderCommand.super.getPriority();
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
            CoordinateSystem.TOP_LEFT_BASED,
            new Insets(0, 0, 0, 0),
            new Insets(2, 2, 2, 2)
        );
    }
}
