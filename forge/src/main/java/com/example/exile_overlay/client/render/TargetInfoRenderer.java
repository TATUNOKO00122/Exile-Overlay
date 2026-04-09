package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.util.MineAndSlashHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TargetInfoRenderer implements IRenderCommand, IHudRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetInfoRenderer.class);
    private static final String COMMAND_ID = "target_info";
    private static final int PRIORITY = 80;

    private static final double MAX_DISTANCE = 64.0;
    private static final long RETENTION_MS = 1000;

    private WeakReference<LivingEntity> lastTargetRef = new WeakReference<>(null);
    private long lastTargetTime = 0;

    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/target_hp_bar_frame.png");

    private static final int TEX_WIDTH = 224;
    private static final int TEX_HEIGHT = 32;

    private static final int EFFECT_ROW_HEIGHT = 20;
    private static final int EFFECT_ICON_SIZE = 16;
    private static final int EFFECT_SPACING = 18;
    private static final int EFFECT_PADDING_X = 5;
    private static final int EFFECT_PADDING_Y = 2;
    private static final int MAX_EFFECTS_PER_ROW = 12;

    private static final int BAR_X = 5;
    private static final int BAR_Y = 20;
    private static final int BAR_WIDTH = 213;
    private static final int BAR_HEIGHT = 10;

    private static final int HP_BAR_COLOR = 0xFFCC2020;
    private static final int HP_BG_COLOR = 0x80000000;
    private static final int ELITE_BAR_COLOR = 0xFFFF4400;
    private static final int BOSS_BAR_COLOR = 0xFFDD0000;

    private static final int NAME_Y = 22;

    public TargetInfoRenderer() {
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

        LivingEntity target = getTargetEntity(mc.player, MAX_DISTANCE);
        long now = System.currentTimeMillis();

        if (target != null) {
            if (target != lastTargetRef.get()) {
                lastTargetRef = new WeakReference<>(target);
            }
            lastTargetTime = now;
        } else {
            target = lastTargetRef.get();
            if (target == null || !target.isAlive() || (now - lastTargetTime) > RETENTION_MS) {
                return;
            }
        }

        int mnsLevel = MineAndSlashHelper.getEntityLevel(target);
        float health = MineAndSlashHelper.getEntityHealth(target);
        float maxHealth = MineAndSlashHelper.getEntityMaxHealth(target);

        MineAndSlashHelper.MobRarityInfo rarity = MineAndSlashHelper.getMobRarity(target);
        List<MineAndSlashHelper.MobAffixInfo> affixes = MineAndSlashHelper.getMobAffixes(target);
        List<MineAndSlashHelper.MobEffectInfo> effects = MineAndSlashHelper.getMobStatusEffects(target);

        String affixPrefix = buildAffixPrefix(affixes);
        String vanillaName = target.getDisplayName().getString();
        if (vanillaName.isEmpty()) {
            return;
        }

        String displayName;
        if (mnsLevel > 0) {
            displayName = affixPrefix + vanillaName;
        } else {
            displayName = affixPrefix + vanillaName;
        }

        String levelText = mnsLevel > 0 ? "Lv." + mnsLevel : "";

        int nameColor = rarity != null ? (0xFF000000 | rarity.color) : 0xFFFFFFFF;
        int barColor = resolveBarColor(rarity, health, maxHealth);
        float hpRatio = Mth.clamp(health / maxHealth, 0.0f, 1.0f);

        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();

        HudPosition position = getPosition();
        int[] pos = position.resolve(screenWidth, screenHeight);
        float scale = getScale();

        int scaledWidth = (int) (TEX_WIDTH * scale);
        int scaledHeight = (int) (TEX_HEIGHT * scale);

        int x = pos[0] - scaledWidth / 2;
        int y = pos[1] - scaledHeight / 2;

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            renderHpBar(graphics, hpRatio, barColor);
            graphics.blit(FRAME_TEXTURE, 0, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            renderNameAndLevel(graphics, mc, displayName, levelText, nameColor);
            renderHpText(graphics, mc, health, maxHealth, hpRatio);
            renderEffects(graphics, mc, effects);
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            graphics.pose().popPose();
        }
    }

    private void renderHpBar(GuiGraphics graphics, float hpRatio, int barColor) {
        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, HP_BG_COLOR);

        int filledWidth = (int) (BAR_WIDTH * hpRatio);
        if (filledWidth > 0) {
            graphics.fill(BAR_X, BAR_Y, BAR_X + filledWidth, BAR_Y + BAR_HEIGHT, barColor);
        }
    }

    private void renderNameAndLevel(GuiGraphics graphics, Minecraft mc, String displayName, String levelText, int nameColor) {
        String combinedName;
        if (!levelText.isEmpty()) {
            combinedName = levelText + " " + displayName;
        } else {
            combinedName = displayName;
        }
        int nameWidth = mc.font.width(combinedName);
        int textX = (TEX_WIDTH - nameWidth) / 2;
        graphics.drawString(mc.font, combinedName, textX, NAME_Y, nameColor, true);
    }

    private void renderHpText(GuiGraphics graphics, Minecraft mc, float health, float maxHealth, float hpRatio) {
        String hpText = formatHpText(health, maxHealth);
        int hpWidth = mc.font.width(hpText);
        int hpX = BAR_X + BAR_WIDTH - hpWidth - 2;
        int hpY = BAR_Y - 9;

        int textColor = hpRatio > 0.5f ? 0xFFFFFFFF : (hpRatio > 0.25f ? 0xFFFFFF00 : 0xFFFF4444);
        graphics.drawString(mc.font, hpText, hpX, hpY, textColor, true);
    }

    private void renderEffects(GuiGraphics graphics, Minecraft mc, List<MineAndSlashHelper.MobEffectInfo> effects) {
        if (effects.isEmpty()) return;

        int drawX = EFFECT_PADDING_X;
        int drawY = TEX_HEIGHT + EFFECT_PADDING_Y;

        int count = Math.min(effects.size(), MAX_EFFECTS_PER_ROW);
        for (int i = 0; i < count; i++) {
            MineAndSlashHelper.MobEffectInfo effect = effects.get(i);
            if (effect.isExpired()) continue;
            int iconX = drawX + i * EFFECT_SPACING;

            if (effect.texture != null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blit(effect.texture, iconX, drawY, 0, 0, EFFECT_ICON_SIZE, EFFECT_ICON_SIZE,
                        EFFECT_ICON_SIZE, EFFECT_ICON_SIZE);
            }

            if (effect.stacks > 1) {
                String stackText = String.valueOf(effect.stacks);
                int stackX = iconX + EFFECT_ICON_SIZE - mc.font.width(stackText) - 1;
                int stackY = drawY + EFFECT_ICON_SIZE - 9;
                graphics.drawString(mc.font, stackText, stackX, stackY, 0xFFFFFFFF, true);
            }

            String durText = effect.getDurationText();
            if (!durText.isEmpty()) {
                int durWidth = mc.font.width(durText);
                int durX = iconX + (EFFECT_ICON_SIZE - durWidth) / 2;
                int durY = drawY + EFFECT_ICON_SIZE + 1;
                graphics.drawString(mc.font, durText, durX, durY, 0xFFAAAAAA, false);
            }
        }
    }

    private String buildAffixPrefix(List<MineAndSlashHelper.MobAffixInfo> affixes) {
        if (affixes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (MineAndSlashHelper.MobAffixInfo affix : affixes) {
            if (affix.name == null || affix.name.isEmpty()) continue;
            if (!affix.icon.isEmpty()) {
                sb.append(affix.icon);
            }
            sb.append(affix.name).append(" ");
        }
        return sb.toString();
    }

    private int resolveBarColor(MineAndSlashHelper.MobRarityInfo rarity, float health, float maxHealth) {
        if (rarity != null) {
            if (rarity.isSpecial) return BOSS_BAR_COLOR;
            if (rarity.isElite) return ELITE_BAR_COLOR;
            if ("uncommon".equals(rarity.id)) return 0xFF44CC44;
        }
        return HP_BAR_COLOR;
    }

    private String formatHpText(float health, float maxHealth) {
        return formatNumber(health) + "/" + formatNumber(maxHealth);
    }

    private String formatNumber(float value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.1f", value);
        }
    }

    private static LivingEntity getTargetEntity(Player player, double maxDistance) {
        try {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

            AABB searchBox = player.getBoundingBox()
                    .expandTowards(lookVec.scale(maxDistance))
                    .inflate(1.0);

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

    @Override
    public String getConfigKey() {
        return "target_mob_name";
    }

    @Override
    public int getWidth() {
        return TEX_WIDTH;
    }

    @Override
    public int getHeight() {
        return TEX_HEIGHT;
    }

    @Override
    public boolean isDraggable() {
        return true;
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
