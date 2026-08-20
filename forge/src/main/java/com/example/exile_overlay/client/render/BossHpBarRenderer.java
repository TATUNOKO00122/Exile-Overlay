package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.api.data.MobAffixInfo;
import com.example.exile_overlay.api.data.MobEffectInfo;
import com.example.exile_overlay.api.data.MobRarityInfo;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.example.exile_overlay.mixin.AccessorBossHealthOverlay;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BossHpBarRenderer implements IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BossHpBarRenderer.class);
    
    private static final TagKey<EntityType<?>> TAG_BLUE_SKIES_BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("blue_skies", "bosses"));
    private static final TagKey<EntityType<?>> TAG_FORGE_BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "bosses"));
    private static final TagKey<EntityType<?>> TAG_FABRIC_BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("c", "bosses"));

    private static final String COMMAND_ID = "boss_hp_bar";
    private static final int PRIORITY = 90;

    private static final ResourceLocation BACKGROUND_TEXTURE =
            new ResourceLocation("exile_overlay", "textures/gui/boss_hp_bar_background.png");
    private static final ResourceLocation FROST_TEXTURE =
            new ResourceLocation("exile_overlay", "textures/gui/boss_hp_bar_frost.png");
    private static final ResourceLocation FRAME_TEXTURE =
            new ResourceLocation("exile_overlay", "textures/gui/boss_hp_bar_frame.png");
    private static final ResourceLocation EFFECT_FRAME_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/effect_icon_frame.png");
    private static final ResourceLocation EFFECT_BACKGROUND_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/effect_icon_background.png");

    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 128;

    private static final int BAR_X = 53;
    private static final int BAR_Y = 55;
    private static final int BAR_WIDTH = 406;
    private static final int BAR_HEIGHT = 20;

    private static final int HP_BAR_COLOR = 0xFFB02020;
    private static final int HP_BG_COLOR = 0x80000000;
    private static final int DEFAULT_BOSS_NAME_COLOR = 0xFFFFAA00;
    private static final int HP_TEXT_COLOR = 0xFFFFFFFF;
    private static final int HP_TEXT_Y = 50;
    private static final int HP_TEXT_RIGHT_X = 431;

    private static final float NAME_TEXT_SCALE = 1.0f;
    private static final float LEVEL_TEXT_SCALE = 0.85f;
    private static final float HP_TEXT_SCALE = 0.9f;
    private static final int NAME_Y = 61;

    private static final int BOSS_EFFECT_ICON_SIZE = 12;
    private static final int BOSS_EFFECT_FRAME_SIZE = 14;
    private static final int BOSS_EFFECT_FRAME_OFFSET = 1;
    private static final int BOSS_EFFECT_SPACING = 15;
    private static final int BOSS_EFFECT_PADDING_Y = 2;
    private static final int BOSS_MAX_EFFECTS_PER_ROW = 20;

    private static final double MS_BOSS_SCAN_RANGE = 128.0;
    private static final long SCAN_INTERVAL_MS = 500;

    private final EquipmentDisplayConfig equipConfig = EquipmentDisplayConfig.getInstance();

    private static WeakReference<LivingEntity> combatMsBoss = new WeakReference<>(null);
    private static WeakReference<LivingEntity> combatVanillaBoss = new WeakReference<>(null);
    private static UUID lastTrackedBossEventId = null;
    private static long lastMsScanTime = 0;
    private static long lastVanillaScanTime = 0;

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
        if (mc.screen instanceof DraggableHudConfigScreen) {
            return true;
        }
        return isVanillaBossVisible(mc) || findNearbyMsBoss(mc) != null;
    }

    public static boolean isMsBossVisible() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        return !hasActiveBossEvent(mc) && findNearbyMsBoss(mc) != null;
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (hasActiveBossEvent(mc)) {
            renderVanillaBoss(graphics, ctx, mc);
        } else {
            renderMsBoss(graphics, ctx, mc);
        }
    }

    private void renderVanillaBoss(GuiGraphics graphics, RenderContext ctx, Minecraft mc) {
        LerpingBossEvent bossEvent = getFirstBossEvent(mc);
        if (bossEvent == null) {
            return;
        }

        float progress = Mth.clamp(bossEvent.getProgress(), 0.0f, 1.0f);
        String bossName = bossEvent.getName().getString();
        if (bossName.isEmpty()) {
            return;
        }

        LivingEntity bossEntity = combatVanillaBoss.get();
        if (bossEntity == null || !bossEntity.isAlive()) {
            bossEntity = findBossEntity(mc, bossEvent);
            if (bossEntity != null) {
                combatVanillaBoss = new WeakReference<>(bossEntity);
            }
        }

        int mnsLevel = bossEntity != null ? MethodHandlesUtil.getEntityLevel(bossEntity) : 0;
        float health = 0;
        float maxHealth = 0;
        if (bossEntity != null) {
            if (MethodHandlesUtil.isAvailable()) {
                try {
                    health = MethodHandlesUtil.getCurrentHealth(bossEntity);
                } catch (Throwable t) {
                    health = bossEntity.getHealth();
                }
                try {
                    maxHealth = MethodHandlesUtil.getMaxHealth(bossEntity);
                } catch (Throwable t) {
                    maxHealth = bossEntity.getMaxHealth();
                }
            } else {
                health = bossEntity.getHealth();
                maxHealth = bossEntity.getMaxHealth();
            }
        } else {
            health = Math.round(progress * 100.0f);
            maxHealth = 100.0f;
        }

        float targetRatio = maxHealth > 0 ? Mth.clamp(health / maxHealth, 0.0f, 1.0f) : progress;
        renderBossBar(graphics, ctx, mc, bossName, mnsLevel, health, maxHealth, targetRatio, DEFAULT_BOSS_NAME_COLOR,
                bossEntity != null ? MethodHandlesUtil.getMobStatusEffectsInfo(bossEntity) : List.of());
    }

    private void renderMsBoss(GuiGraphics graphics, RenderContext ctx, Minecraft mc) {
        LivingEntity bossEntity = findNearbyMsBoss(mc);
        if (bossEntity == null) {
            if (mc.screen instanceof DraggableHudConfigScreen) {
                bossEntity = mc.player;
            } else {
                return;
            }
        }

        String vanillaName = bossEntity.getDisplayName().getString();
        if (vanillaName.isEmpty()) {
            return;
        }

        int mnsLevel = MethodHandlesUtil.getEntityLevel(bossEntity);
        float health;
        float maxHealth;
        try {
            health = MethodHandlesUtil.getCurrentHealth(bossEntity);
        } catch (Throwable t) {
            health = bossEntity.getHealth();
        }
        try {
            maxHealth = MethodHandlesUtil.getMaxHealth(bossEntity);
        } catch (Throwable t) {
            maxHealth = bossEntity.getMaxHealth();
        }

        float progress = maxHealth > 0 ? Mth.clamp(health / maxHealth, 0.0f, 1.0f) : 0.0f;

        List<MobAffixInfo> affixes = MethodHandlesUtil.getMobAffixesInfo(bossEntity);
        String displayName = TargetInfoRenderer.formatMobDisplayName(vanillaName, affixes);

        MobRarityInfo rarity = MethodHandlesUtil.getMobRarityInfo(bossEntity);
        List<MobEffectInfo> effects = MethodHandlesUtil.getMobStatusEffectsInfo(bossEntity);
        renderBossBar(graphics, ctx, mc, displayName, mnsLevel, health, maxHealth, progress, DEFAULT_BOSS_NAME_COLOR, effects);
    }

    private void renderBossBar(GuiGraphics graphics, RenderContext ctx, Minecraft mc,
                                String bossName, int mnsLevel, float health, float maxHealth,
                                float progress, int nameColor, List<MobEffectInfo> effects) {
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

            graphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            renderHpBar(graphics, progress);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(FROST_TEXTURE, 0, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            graphics.blit(FRAME_TEXTURE, 0, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            renderBossName(graphics, mc, bossName, mnsLevel, nameColor);
            renderHpText(graphics, mc, health, maxHealth);
            if (equipConfig.isShowBossMobEffects()) {
                renderEffects(graphics, mc, effects);
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            graphics.pose().popPose();
        }
    }

    private void renderHpBar(GuiGraphics graphics, float hpRatio) {
        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, HP_BG_COLOR);
        int filledWidth = (int) (BAR_WIDTH * hpRatio);
        if (filledWidth > 0) {
            graphics.fill(BAR_X, BAR_Y, BAR_X + filledWidth, BAR_Y + BAR_HEIGHT, HP_BAR_COLOR);
        }
    }

    private void renderBossName(GuiGraphics graphics, Minecraft mc, String bossName, int level, int nameColor) {
        float totalWidth;
        if (level > 0) {
            String levelText = "Lv." + level + " ";
            float levelW = mc.font.width(levelText) * LEVEL_TEXT_SCALE;
            float nameW = mc.font.width(bossName) * NAME_TEXT_SCALE;
            totalWidth = levelW + nameW;
        } else {
            totalWidth = mc.font.width(bossName) * NAME_TEXT_SCALE;
        }

        float textX = (TEX_WIDTH - totalWidth) / 2.0f;
        float textY = NAME_Y;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);

        if (level > 0) {
            String levelText = "Lv." + level + " ";
            graphics.pose().pushPose();
            graphics.pose().translate(0, 1, 0);
            graphics.pose().scale(LEVEL_TEXT_SCALE, LEVEL_TEXT_SCALE, 1.0f);
            graphics.drawString(mc.font, levelText, 0, 0, nameColor, true);
            graphics.pose().popPose();
            float levelW = mc.font.width(levelText) * LEVEL_TEXT_SCALE;
            graphics.pose().translate(levelW, 0, 0);
        }

        graphics.pose().scale(NAME_TEXT_SCALE, NAME_TEXT_SCALE, 1.0f);
        graphics.drawString(mc.font, bossName, 0, 0, nameColor, true);
        graphics.pose().popPose();
    }

    private void renderHpText(GuiGraphics graphics, Minecraft mc, float health, float maxHealth) {
        String hpText = formatNumber(health) + "/" + formatNumber(maxHealth);
        float textWidth = HudFontHelper.getTextWidth(mc.font, hpText) * HP_TEXT_SCALE;
        float hpX = HP_TEXT_RIGHT_X - textWidth;

        graphics.pose().pushPose();
        graphics.pose().translate(hpX, HP_TEXT_Y - mc.font.lineHeight * HP_TEXT_SCALE, 0);
        graphics.pose().scale(HP_TEXT_SCALE, HP_TEXT_SCALE, 1.0f);
        HudFontHelper.drawString(graphics, mc.font, hpText, 0, 0, HP_TEXT_COLOR, true);
        graphics.pose().popPose();
    }

    private void renderEffects(GuiGraphics graphics, Minecraft mc, List<MobEffectInfo> effects) {
        if (effects.isEmpty()) return;

        List<MobEffectInfo> activeEffects = effects.stream()
                .filter(e -> !e.isExpired())
                .limit(BOSS_MAX_EFFECTS_PER_ROW)
                .toList();
        if (activeEffects.isEmpty()) return;

        int drawX = 77;
        int drawY = TEX_HEIGHT - 49 + 1;
        float textScale = 0.7f;

        for (int i = 0; i < activeEffects.size(); i++) {
            MobEffectInfo effect = activeEffects.get(i);
            int iconX = drawX + i * BOSS_EFFECT_SPACING;

            RenderSystem.enableBlend();
            graphics.blit(EFFECT_BACKGROUND_TEXTURE,
                    iconX - BOSS_EFFECT_FRAME_OFFSET, drawY - BOSS_EFFECT_FRAME_OFFSET,
                    0, 0, BOSS_EFFECT_FRAME_SIZE, BOSS_EFFECT_FRAME_SIZE,
                    BOSS_EFFECT_FRAME_SIZE, BOSS_EFFECT_FRAME_SIZE);

            if (effect.texture != null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blit(effect.texture, iconX, drawY, 0, 0,
                    BOSS_EFFECT_ICON_SIZE, BOSS_EFFECT_ICON_SIZE,
                    BOSS_EFFECT_ICON_SIZE, BOSS_EFFECT_ICON_SIZE);
            }

            RenderSystem.enableBlend();
            graphics.blit(EFFECT_FRAME_TEXTURE,
                    iconX - BOSS_EFFECT_FRAME_OFFSET, drawY - BOSS_EFFECT_FRAME_OFFSET,
                    0, 0, BOSS_EFFECT_FRAME_SIZE, BOSS_EFFECT_FRAME_SIZE,
                    BOSS_EFFECT_FRAME_SIZE, BOSS_EFFECT_FRAME_SIZE);

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
                int durX = iconX + (BOSS_EFFECT_ICON_SIZE - durWidth) / 2;
                int durY = drawY + BOSS_EFFECT_ICON_SIZE + 1;
                graphics.pose().pushPose();
                graphics.pose().translate(durX, durY, 0);
                graphics.pose().scale(textScale, textScale, 1.0f);
                HudFontHelper.drawString(graphics, mc.font, durText, 0, 0, 0xFFAAAAAA, false);
                graphics.pose().popPose();
            }
        }
    }

    private static String formatNumber(float value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    private static LivingEntity findBossEntity(Minecraft mc, LerpingBossEvent bossEvent) {
        if (mc.level == null || mc.player == null || bossEvent == null) return null;

        float targetProgress = Mth.clamp(bossEvent.getProgress(), 0.0f, 1.0f);

        AABB searchBox = new AABB(
                mc.player.getX() - MS_BOSS_SCAN_RANGE, mc.player.getY() - MS_BOSS_SCAN_RANGE, mc.player.getZ() - MS_BOSS_SCAN_RANGE,
                mc.player.getX() + MS_BOSS_SCAN_RANGE, mc.player.getY() + MS_BOSS_SCAN_RANGE, mc.player.getZ() + MS_BOSS_SCAN_RANGE);

        List<LivingEntity> bossCandidates = mc.level.getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e != null && e.isAlive() && isAnyBoss(e));

        if (bossCandidates.isEmpty()) {
            bossCandidates = mc.level.getEntitiesOfClass(
                    LivingEntity.class, searchBox,
                    e -> e != null && e.isAlive() && !(e instanceof Player));
        }

        if (bossCandidates.isEmpty()) {
            return null;
        }

        return bossCandidates.stream()
                .min(Comparator
                        .comparingDouble((LivingEntity e) -> getHpProgressDiff(e, targetProgress))
                        .thenComparingDouble(e -> e.distanceToSqr(mc.player)))
                .orElse(null);
    }

    private static double getHpProgressDiff(LivingEntity entity, float targetProgress) {
        float health;
        float maxHealth;
        if (MethodHandlesUtil.isAvailable()) {
            try {
                health = MethodHandlesUtil.getCurrentHealth(entity);
            } catch (Throwable t) {
                health = entity.getHealth();
            }
            try {
                maxHealth = MethodHandlesUtil.getMaxHealth(entity);
            } catch (Throwable t) {
                maxHealth = entity.getMaxHealth();
            }
        } else {
            health = entity.getHealth();
            maxHealth = entity.getMaxHealth();
        }

        if (maxHealth <= 0) return 1.0;
        float ratio = Mth.clamp(health / maxHealth, 0.0f, 1.0f);
        return Math.abs(ratio - targetProgress);
    }

    private static LivingEntity findNearbyMsBoss(Minecraft mc) {
        if (!MethodHandlesUtil.isAvailable() || mc.player == null || mc.level == null) {
            return null;
        }

        LivingEntity active = combatMsBoss.get();
        if (active != null && active.isAlive() && active.level() == mc.level) {
            double distSq = active.distanceToSqr(mc.player);
            if (distSq <= MS_BOSS_SCAN_RANGE * MS_BOSS_SCAN_RANGE) {
                return active;
            }
            combatMsBoss = new WeakReference<>(null);
            return null;
        }
        if (active != null) {
            combatMsBoss = new WeakReference<>(null);
        }

        long now = System.currentTimeMillis();
        if (now - lastMsScanTime < SCAN_INTERVAL_MS) {
            return null;
        }
        lastMsScanTime = now;

        AABB searchBox = new AABB(
                mc.player.getX() - MS_BOSS_SCAN_RANGE, mc.player.getY() - MS_BOSS_SCAN_RANGE, mc.player.getZ() - MS_BOSS_SCAN_RANGE,
                mc.player.getX() + MS_BOSS_SCAN_RANGE, mc.player.getY() + MS_BOSS_SCAN_RANGE, mc.player.getZ() + MS_BOSS_SCAN_RANGE);
        List<LivingEntity> bosses = mc.level.getEntitiesOfClass(
                LivingEntity.class, searchBox,
                BossHpBarRenderer::isAnyBoss);

        if (bosses.isEmpty()) {
            return null;
        }

        bosses.sort(Comparator.comparingDouble(b -> b.distanceToSqr(mc.player)));
        for (LivingEntity boss : bosses) {
            if (boss.isAlive() && hasLineOfSight(mc, boss)) {
                combatMsBoss = new WeakReference<>(boss);
                return boss;
            }
        }

        return null;
    }

    static boolean isAnyBoss(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;

        if (isMsBoss(entity)) return true;

        if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
            return true;
        }

        try {
            String className = entity.getClass().getName().toLowerCase();
            if (className.contains("blue_skies") && className.contains("boss")) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        try {
            EntityType<?> type = entity.getType();
            if (type.is(TAG_BLUE_SKIES_BOSSES) || type.is(TAG_FORGE_BOSSES) || type.is(TAG_FABRIC_BOSSES)) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        try {
            if (entity.getPersistentData().getBoolean("isUberBoss") ||
                    entity.getPersistentData().getBoolean("isFinalMapBoss") ||
                    entity.getPersistentData().getBoolean("isBoss")) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    static boolean isMsBoss(LivingEntity entity) {
        if (!MethodHandlesUtil.isAvailable() || entity == null) return false;
        MobRarityInfo rarity = MethodHandlesUtil.getMobRarityInfo(entity);
        if (rarity == null) return false;
        return "boss".equals(rarity.id) || "uber".equals(rarity.id) || "pinnacle".equals(rarity.id);
    }

    private static boolean hasActiveBossEvent(Minecraft mc) {
        try {
            BossHealthOverlay overlay = mc.gui.getBossOverlay();
            Map<UUID, LerpingBossEvent> events =
                    ((AccessorBossHealthOverlay) (Object) overlay).exileOverlay$getEvents();
            return events.values().stream().anyMatch(e -> !isExcludedBossEvent(e));
        } catch (Exception e) {
            return false;
        }
    }

    private static LerpingBossEvent getFirstBossEvent(Minecraft mc) {
        try {
            BossHealthOverlay overlay = mc.gui.getBossOverlay();
            Map<UUID, LerpingBossEvent> events =
                    ((AccessorBossHealthOverlay) (Object) overlay).exileOverlay$getEvents();
            if (events == null || events.isEmpty()) {
                lastTrackedBossEventId = null;
                return null;
            }

            // 直前に追跡していたイベントがまだ有効であれば優先して返却（フリッカー防止）
            if (lastTrackedBossEventId != null) {
                LerpingBossEvent existing = events.get(lastTrackedBossEventId);
                if (existing != null && !isExcludedBossEvent(existing)) {
                    return existing;
                }
            }

            for (Map.Entry<UUID, LerpingBossEvent> entry : events.entrySet()) {
                if (!isExcludedBossEvent(entry.getValue())) {
                    lastTrackedBossEventId = entry.getKey();
                    return entry.getValue();
                }
            }

            lastTrackedBossEventId = null;
            return null;
        } catch (Exception e) {
            LOGGER.error("exile_overlay/BossHpBar: Failed to get boss event", e);
            lastTrackedBossEventId = null;
            return null;
        }
    }

    private static boolean isExcludedBossEvent(LerpingBossEvent event) {
        String name = event.getName().getString();
        return name.startsWith("GATEWAY_ID");
    }

    // ========== Line of Sight & Combat State ==========

    /**
     * プレイヤーからターゲットまで壁がないかレイキャストで確認
     */
    private static boolean hasLineOfSight(Minecraft mc, LivingEntity target) {
        if (mc.player == null || mc.level == null) return false;
        Vec3 playerEye = mc.player.getEyePosition(1.0f);
        Vec3 targetEye = target.getEyePosition(1.0f);
        ClipContext context = new ClipContext(
                playerEye, targetEye,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        );
        BlockHitResult result = mc.level.clip(context);
        return result.getType() == HitResult.Type.MISS;
    }

    /**
     * Vanilla BOSSイベントの視線ベース戦闘状態管理。
     * 初回はレイキャストで視線確認 → 以降はBOSS死亡/範囲外まで表示継続。
     */
    public static boolean isVanillaBossVisible(Minecraft mc) {
        if (!hasActiveBossEvent(mc)) {
            combatVanillaBoss = new WeakReference<>(null);
            lastTrackedBossEventId = null;
            return false;
        }

        LivingEntity active = combatVanillaBoss.get();
        if (active != null && active.isAlive() && active.level() == mc.level) {
            double distSq = active.distanceToSqr(mc.player);
            if (distSq <= MS_BOSS_SCAN_RANGE * MS_BOSS_SCAN_RANGE) {
                return true;
            }
            combatVanillaBoss = new WeakReference<>(null);
            return false;
        }
        if (active != null) {
            combatVanillaBoss = new WeakReference<>(null);
        }

        long now = System.currentTimeMillis();
        if (now - lastVanillaScanTime < SCAN_INTERVAL_MS) {
            return false;
        }
        lastVanillaScanTime = now;

        LerpingBossEvent event = getFirstBossEvent(mc);
        if (event == null) return false;

        LivingEntity entity = findBossEntity(mc, event);
        if (entity != null && entity.isAlive() && hasLineOfSight(mc, entity)) {
            combatVanillaBoss = new WeakReference<>(entity);
            return true;
        }

        return false;
    }

    @Override
    public int getConfigWidth() {
        return TEX_WIDTH - 64;
    }

    @Override
    public int getConfigHeight() {
        return TEX_HEIGHT - 64;
    }

    @Override
    public String getConfigKey() {
        return "boss_hp_bar";
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
