package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.example.exile_overlay.client.render.entity.EntityHealthBarConfig;
import com.example.exile_overlay.api.data.AffixStatInfo;
import com.example.exile_overlay.api.data.MobAffixInfo;
import com.example.exile_overlay.api.data.MobEffectInfo;
import com.example.exile_overlay.api.data.MobRarityInfo;
import com.example.exile_overlay.client.render.ailment.ClientAilmentTracker;
import com.example.exile_overlay.util.TopDownViewHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TargetInfoRenderer implements IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetInfoRenderer.class);
    private static final String COMMAND_ID = "target_info";
    private static final int PRIORITY = 80;

    private static final double MAX_DISTANCE = 64.0;
    private static final long RETENTION_MS = 1000;

    private WeakReference<LivingEntity> lastTargetRef = new WeakReference<>(null);
    private long lastTargetTime = 0;

    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/target_hp_bar_frame.png");
    private static final ResourceLocation EFFECT_FRAME_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/effect_icon_frame.png");
    private static final ResourceLocation EFFECT_BACKGROUND_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/effect_icon_background.png");

    private static final int TEX_WIDTH = 224;
    private static final int TEX_HEIGHT = 32;

    private static final int EFFECT_ROW_HEIGHT = 15;
    private static final int EFFECT_ICON_SIZE = 11;
    private static final int EFFECT_FRAME_SIZE = 13;
    private static final int EFFECT_FRAME_OFFSET = 1;
    private static final int EFFECT_SPACING = 15;
    private static final int EFFECT_PADDING_X = 5;
    private static final int EFFECT_PADDING_Y = 2;
    private static final int MAX_EFFECTS_PER_ROW = 11;
    private static final int MAX_AFFIX_STATS_DISPLAY = 5;
    private static final int AFFIX_STAT_LINE_HEIGHT = 6;
    private static final float AFFIX_STAT_SCALE = 0.65f;

    private static final int BAR_X = 5;
    private static final int BAR_Y = 20;
    private static final int BAR_WIDTH = 213;
    private static final int BAR_HEIGHT = 10;

    private static final int HP_BG_COLOR = 0x80000000;

    private static final int NAME_Y = 22;

    private final EquipmentDisplayConfig equipConfig = EquipmentDisplayConfig.getInstance();
    private final EntityHealthBarConfig hpBarConfig = EntityHealthBarConfig.getInstance();

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
        if (!IRenderCommand.super.isVisible(ctx)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        // BossHpBar が実際に表示されている時はターゲット情報を非表示（画面上の重複・競合防止）
        if (BossHpBarRenderer.isVanillaBossVisible(mc) || BossHpBarRenderer.isMsBossVisible()) {
            return false;
        }
        return true;
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
            if (target == null || !target.isAlive() || (now - lastTargetTime) > RETENTION_MS || !mc.player.hasLineOfSight(target)) {
                if (mc.screen instanceof DraggableHudConfigScreen) {
                    target = mc.player;
                } else {
                    return;
                }
            }
        }

        int mnsLevel = MethodHandlesUtil.getEntityLevel(target);
        float health;
        float maxHealth;
        if (MethodHandlesUtil.isAvailable()) {
            try {
                health = MethodHandlesUtil.getCurrentHealth(target);
            } catch (Throwable t) {
                health = target.getHealth();
            }
            try {
                maxHealth = MethodHandlesUtil.getMaxHealth(target);
            } catch (Throwable t) {
                maxHealth = target.getMaxHealth();
            }
        } else {
            health = target.getHealth();
            maxHealth = target.getMaxHealth();
        }

        MobRarityInfo rarity = MethodHandlesUtil.getMobRarityInfo(target);
        List<MobAffixInfo> affixes = MethodHandlesUtil.getMobAffixesInfo(target);
        List<MobEffectInfo> effects = MethodHandlesUtil.getMobStatusEffectsInfo(target);

        String vanillaName = target.getDisplayName().getString();
        if (vanillaName.isEmpty()) {
            return;
        }

        String displayName = formatMobDisplayName(vanillaName, affixes);

        String levelText = mnsLevel > 0 ? "Lv." + mnsLevel : "";

        int nameColor = rarity != null ? (0xFF000000 | rarity.color) : 0xFFFFFFFF;
        int barColor = resolveBarColor(rarity, target);
        float hpRatio = (maxHealth > 0.0f && !Float.isNaN(health) && !Float.isNaN(maxHealth)) ? Mth.clamp(health / maxHealth, 0.0f, 1.0f) : 0.0f;
        float bloodLossEndRatio = hpBarConfig.isShowBleed()
                ? ClientAilmentTracker.getInstance().getBloodLossEndRatio(target, hpRatio, maxHealth)
                : hpRatio;

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

            renderHpBar(graphics, hpRatio, bloodLossEndRatio, barColor);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(FRAME_TEXTURE, 0, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            renderNameAndLevel(graphics, mc, displayName, levelText, nameColor);
            renderHpText(graphics, mc, health, maxHealth);
            if (equipConfig.isShowTargetMobEffects()) {
                renderEffects(graphics, mc, effects);
            }
            if (equipConfig.isShowTargetAffixStats()) {
                renderAffixStats(graphics, mc, affixes);
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            graphics.pose().popPose();
        }
    }

    private void renderHpBar(GuiGraphics graphics, float hpRatio, float bloodLossEndRatio, int barColor) {
        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, HP_BG_COLOR);

        int currentHpWidth = (int) (BAR_WIDTH * hpRatio);
        int bloodEndWidth = (int) (BAR_WIDTH * bloodLossEndRatio);

        // PoE2仕様: 出血（失血）蓄積バー (暗赤色)
        if (bloodEndWidth > currentHpWidth) {
            graphics.fill(BAR_X + currentHpWidth, BAR_Y, BAR_X + bloodEndWidth, BAR_Y + BAR_HEIGHT, hpBarConfig.getBleedBarColorHex());
        }

        // 現在HPバー (通常時は赤、毒状態時は深緑)
        if (currentHpWidth > 0) {
            graphics.fill(BAR_X, BAR_Y, BAR_X + currentHpWidth, BAR_Y + BAR_HEIGHT, barColor);
        }
    }

    private static final float NAME_TEXT_SCALE = 0.945f;

    private void renderNameAndLevel(GuiGraphics graphics, Minecraft mc, String displayName, String levelText, int nameColor) {
        String combinedName;
        if (!levelText.isEmpty()) {
            combinedName = levelText + " " + displayName;
        } else {
            combinedName = displayName;
        }
        float nameWidth = mc.font.width(combinedName) * NAME_TEXT_SCALE;
        float textX = (TEX_WIDTH - nameWidth) / 2.0f;
        float textY = NAME_Y + (mc.font.lineHeight * (1.0f - NAME_TEXT_SCALE)) / 2.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(NAME_TEXT_SCALE, NAME_TEXT_SCALE, 1.0f);
        graphics.drawString(mc.font, combinedName, 0, 0, nameColor, true);
        graphics.pose().popPose();
    }

    private void renderHpText(GuiGraphics graphics, Minecraft mc, float health, float maxHealth) {
        String hpText = formatHpText(health, maxHealth);
        float hpScale = 0.7f;
        float textWidth = HudFontHelper.getTextWidth(mc.font, hpText) * hpScale;
        int hpX = (int) (BAR_X + BAR_WIDTH - textWidth - 2);
        int hpY = BAR_Y - (int) (mc.font.lineHeight * hpScale);

        graphics.pose().pushPose();
        graphics.pose().translate(hpX, hpY - 1.0f, 0);
        graphics.pose().scale(hpScale, hpScale, 1.0f);
        HudFontHelper.drawString(graphics, mc.font, hpText, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private void renderEffects(GuiGraphics graphics, Minecraft mc, List<MobEffectInfo> effects) {
        if (effects.isEmpty()) return;

        int drawX = EFFECT_PADDING_X;
        int drawY = TEX_HEIGHT + EFFECT_PADDING_Y;
        float textScale = 0.7f;

        int count = Math.min(effects.size(), MAX_EFFECTS_PER_ROW);
        for (int i = 0; i < count; i++) {
            MobEffectInfo effect = effects.get(i);
            if (effect.isExpired()) continue;
            int iconX = drawX + i * EFFECT_SPACING;

            RenderSystem.enableBlend();
            graphics.blit(EFFECT_BACKGROUND_TEXTURE,
                    iconX - EFFECT_FRAME_OFFSET, drawY - EFFECT_FRAME_OFFSET,
                    0, 0, EFFECT_FRAME_SIZE, EFFECT_FRAME_SIZE,
                    EFFECT_FRAME_SIZE, EFFECT_FRAME_SIZE);

            if (effect.texture != null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blit(effect.texture, iconX, drawY, 0, 0, EFFECT_ICON_SIZE, EFFECT_ICON_SIZE,
                        EFFECT_ICON_SIZE, EFFECT_ICON_SIZE);
            }

            RenderSystem.enableBlend();
            graphics.blit(EFFECT_FRAME_TEXTURE,
                    iconX - EFFECT_FRAME_OFFSET, drawY - EFFECT_FRAME_OFFSET,
                    0, 0, EFFECT_FRAME_SIZE, EFFECT_FRAME_SIZE,
                    EFFECT_FRAME_SIZE, EFFECT_FRAME_SIZE);

            if (effect.stacks > 1) {
                String stackText = String.valueOf(effect.stacks);
                graphics.pose().pushPose();
                graphics.pose().translate(iconX + 1, drawY + 1, 0);
                graphics.pose().scale(textScale, textScale, 1.0f);
                HudFontHelper.drawString(graphics, mc.font, stackText, 0, 0, 0xFFFFFFFF, true);
                graphics.pose().popPose();
            }

            String durText = effect.getDurationText();
            if (!durText.isEmpty()) {
                int durWidth = (int) (HudFontHelper.getTextWidth(mc.font, durText) * textScale);
                int durX = iconX + (EFFECT_ICON_SIZE - durWidth) / 2;
                int durY = drawY + EFFECT_ICON_SIZE + 1;
                graphics.pose().pushPose();
                graphics.pose().translate(durX, durY, 0);
                graphics.pose().scale(textScale, textScale, 1.0f);
                HudFontHelper.drawString(graphics, mc.font, durText, 0, 0, 0xFFAAAAAA, false);
                graphics.pose().popPose();
            }
        }
    }

    private void renderAffixStats(GuiGraphics graphics, Minecraft mc,
                                   List<MobAffixInfo> affixes) {
        List<AffixStatInfo> allStats = new ArrayList<>();
        for (MobAffixInfo affix : affixes) {
            allStats.addAll(affix.stats);
        }
        if (allStats.isEmpty()) return;

        int startY = TEX_HEIGHT + EFFECT_PADDING_Y;

        int count = Math.min(allStats.size(), MAX_AFFIX_STATS_DISPLAY);
        for (int i = 0; i < count; i++) {
            AffixStatInfo stat = allStats.get(i);
            String text = stat.getDisplayText();
            float textWidth = HudFontHelper.getTextWidth(mc.font, text) * AFFIX_STAT_SCALE;
            int x = (int) (TEX_WIDTH - textWidth - 3);
            int y = startY + (int) (i * AFFIX_STAT_LINE_HEIGHT / AFFIX_STAT_SCALE);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(AFFIX_STAT_SCALE, AFFIX_STAT_SCALE, 1.0f);
            HudFontHelper.drawString(graphics, mc.font, text, 0, 0, AffixStatInfo.DISPLAY_COLOR, true);
            graphics.pose().popPose();
        }
    }

    public static String formatMobDisplayName(String vanillaName, List<MobAffixInfo> affixes) {
        if (vanillaName == null || vanillaName.isEmpty()) {
            return "";
        }
        if (affixes == null || affixes.isEmpty()) {
            return vanillaName;
        }

        StringBuilder prefixSb = new StringBuilder();
        StringBuilder suffixSb = new StringBuilder();

        for (MobAffixInfo affix : affixes) {
            if (affix == null || affix.name == null || affix.name.isEmpty()) continue;
            String rawName = affix.name;
            if (affix.isPrefix) {
                if (prefixSb.length() > 0) {
                    prefixSb.append(", ");
                }
                if (!affix.icon.isEmpty()) {
                    prefixSb.append(affix.icon);
                }
                prefixSb.append(rawName);
            } else {
                if (suffixSb.length() > 0) {
                    suffixSb.append(", ");
                }
                if (!affix.icon.isEmpty()) {
                    suffixSb.append(affix.icon);
                }
                suffixSb.append(rawName);
            }
        }

        String prefixes = prefixSb.toString();
        String suffixes = suffixSb.toString();

        if (prefixes.isEmpty() && suffixes.isEmpty()) {
            return vanillaName;
        }

        if (!prefixes.isEmpty() && !suffixes.isEmpty()) {
            return Component.translatable("exile_overlay.target.affix_format_full", prefixes, vanillaName, suffixes).getString();
        } else if (!prefixes.isEmpty()) {
            return Component.translatable("exile_overlay.target.affix_format_prefix", prefixes, vanillaName).getString();
        } else {
            return Component.translatable("exile_overlay.target.affix_format_suffix", vanillaName, suffixes).getString();
        }
    }

    private int resolveBarColor(MobRarityInfo rarity, LivingEntity target) {
        if (hpBarConfig.isShowPoison() && ClientAilmentTracker.getInstance().isPoisoned(target)) {
            return hpBarConfig.getPoisonBarColorHex();
        }
        if (hpBarConfig.isShowFriendlyColor() && !isHostile(target)) {
            return hpBarConfig.getFriendlyBarColorHex();
        }
        return hpBarConfig.getHostileBarColorHex();
    }

    private static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Enemy) return true;
        if (entity instanceof WitherBoss) return true;
        if (entity instanceof EnderDragon) return true;
        return false;
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
        LivingEntity target = findTargetEntity(player, maxDistance);
        if (target != null && player.hasLineOfSight(target)) {
            return target;
        }
        return null;
    }

    private static LivingEntity findTargetEntity(Player player, double maxDistance) {
        LivingEntity tdvTarget = TopDownViewHelper.getTarget();
        if (tdvTarget != null && tdvTarget.isAlive()) {
            return tdvTarget;
        }

        // バニラのクロスヘアターゲットを優先（バニラ環境やTopDownView無効時のフォールバックとして確実に取得するため）
        Minecraft mc = Minecraft.getInstance();
        if (mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult entityHitResult 
                && entityHitResult.getEntity() instanceof LivingEntity living && living.isAlive()) {
            return living;
        }

        try {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);

            double effectiveDistance = maxDistance;
            HitResult blockHit = player.pick(maxDistance, 1.0f, false);
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                effectiveDistance = Math.min(maxDistance, blockHit.getLocation().distanceTo(eyePos));
            }

            Vec3 endPos = eyePos.add(lookVec.scale(effectiveDistance));

            AABB searchBox = player.getBoundingBox()
                    .expandTowards(lookVec.scale(effectiveDistance))
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
                    effectiveDistance * effectiveDistance);

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
