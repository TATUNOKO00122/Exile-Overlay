package com.example.exile_overlay.client.damage;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DamagePopupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamagePopupManager");
    private static final DamagePopupManager INSTANCE = new DamagePopupManager();

    private final List<DamageNumber> damageNumbers = new ArrayList<>();

    private static final float KNOCKBACK_STRENGTH = 0.06f;
    private static final float SPREAD_RADIUS_BASE = 0.3f;
    private static final float SPREAD_RADIUS_INCREMENT = 0.25f;

    private final DamagePopupConfig config = DamagePopupConfig.getInstance();

    private static final int HEAL_DEDUP_TICKS = 5;
    private int lastHealEntityId = -1;
    private int lastHealTick = -100;

    // ========== M&S処理済みフラグ（二重表示防止） ==========
    // M&SのDamageInformationMixinが処理したエンティティとtickを記録し、
    // setHealth/HPポーリング由来の白いダメージ数値の重複を防ぐ。
    private static final int MS_DEDUP_TICKS = 3;
    private final Map<Integer, Integer> msDamageHandledTick = new HashMap<>();

    // ========== HP Polling (tickベースのダメージ検出) ==========

    private static final int POLLING_RANGE = 64;
    private final Map<Integer, Float> lastKnownHealth = new HashMap<>();
    private final Map<Integer, Integer> lastHealthChangeTick = new HashMap<>();
    private int cleanupCounter = 0;

    private DamagePopupManager() {
    }

    public static DamagePopupManager getInstance() {
        return INSTANCE;
    }

    // ========== Health Change Detection ==========

    public void onHealthChanged(LivingEntity entity, float oldHealth, float newHealth) {
        try {
            if (entity == null || !entity.level().isClientSide()) {
                return;
            }

            // M&S導入時はバニラルートを完全ブロック。
            // DamageInformationMixin・ヒールMixinのパケット経由で全て処理する。
            if (MethodHandlesUtil.isAvailable()) {
                markHealthSync(entity, newHealth);
                return;
            }

            float diff = oldHealth - newHealth;

            if (Math.abs(diff) > 0.1f) {
                markHealthSync(entity, newHealth);
            }

            if (diff > 0.1f) {
                onDamage(entity, diff);
            } else if (diff < -0.1f) {
                onHeal(entity, -diff, true);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle health change", e);
        }
    }

    /**
     * setHealth/Tickポーリングで検出したHP変化を同期マーク。
     * 重複検出防止のため、最後に処理したゲームTick数を記録する。
     */
    public void markHealthSync(LivingEntity entity, float newHealth) {
        int currentTick = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.tickCount : 0;
        int id = entity.getId();
        lastHealthChangeTick.put(id, currentTick);
        lastKnownHealth.put(id, newHealth);
    }

    /**
     * M&SのDamageInformationが処理したエンティティをマーク。
     * MS_DEDUP_TICKS以内のHP変化検出を無視して二重表示を防ぐ。
     */
    public void markMsDamageHandled(int entityId) {
        int currentTick = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.tickCount : 0;
        msDamageHandledTick.put(entityId, currentTick);
    }

    /**
     * 直近のtick内にM&Sが処理済みかどうかを確認する。
     */
    private boolean isMsDamageHandled(int entityId) {
        if (!MethodHandlesUtil.isAvailable()) {
            return false;
        }
        Integer lastMs = msDamageHandledTick.get(entityId);
        if (lastMs == null) {
            return false;
        }
        int currentTick = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.tickCount : 0;
        return currentTick - lastMs <= MS_DEDUP_TICKS;
    }

    /**
     * 攻撃者がプレイヤー自身またはプレイヤーが所有する召喚獣/ペットであるか判定。
     */
    private boolean isPlayerOrPetAttack(LivingEntity entity) {
        if (entity == null) return false;
        net.minecraft.world.entity.LivingEntity attacker = entity.getLastHurtByMob();
        if (attacker == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (attacker == mc.player) {
            return true;
        }

        if (attacker instanceof net.minecraft.world.entity.OwnableEntity ownable) {
            try {
                if (ownable.getOwner() == mc.player) {
                    return true;
                }
            } catch (Exception e) {
                // getOwner() 呼び出しエラーへの保険
            }
        }

        return false;
    }

    /**
     * バニラHP差分をM&Sスケールに変換する。
     * M&S非使用時はそのまま返す。
     */
    private float convertToMsScale(LivingEntity entity, float vanillaAmount) {
        if (!MethodHandlesUtil.isAvailable()) {
            return vanillaAmount;
        }
        try {
            float msMaxHealth = MethodHandlesUtil.getMaxHealth(entity);
            float vanillaMaxHealth = entity.getMaxHealth();
            if (vanillaMaxHealth <= 0 || msMaxHealth <= 0) {
                return vanillaAmount;
            }
            float ratio = msMaxHealth / vanillaMaxHealth;
            if (Math.abs(ratio - 1.0f) < 0.01f) {
                return vanillaAmount;
            }
            return vanillaAmount * ratio;
        } catch (Throwable t) {
            LOGGER.debug("Failed to convert to M&S scale: {}", t.getMessage());
            return vanillaAmount;
        }
    }

    private void onDamage(LivingEntity entity, float damage) {
        if (!config.isShowPlayerDamage() && entity instanceof Player) {
            return;
        }

        boolean isCrit = false;
        if (entity.getLastHurtByMob() instanceof Player player) {
            isCrit = player.getAttackStrengthScale(0.5f) > 0.9f;
        }

        Vec3 basePosition = entity.position().add(0, entity.getBbHeight() * config.getPopupHeightRatio(), 0);
        Vec3 knockback = calculateKnockback(entity);
        addDamageNumber(basePosition, damage, isCrit, DamageType.NORMAL, entity.getId(), knockback);
    }

    private void onHeal(LivingEntity entity, float healAmount, boolean fromVanillaHealth) {
        if (!config.isShowHealing()) {
            return;
        }

        if (entity instanceof Player && !config.isShowPlayerHealing()) {
            return;
        }

        if (fromVanillaHealth) {
            Minecraft mc = Minecraft.getInstance();
            int currentTick = mc.player != null ? (int) mc.player.tickCount : 0;
            int entityId = entity.getId();
            if (entityId == lastHealEntityId && currentTick - lastHealTick < HEAL_DEDUP_TICKS) {
                return;
            }
        }

        Vec3 basePosition = entity.position().add(0, entity.getBbHeight() * config.getPopupHeightRatio(), 0);
        addDamageNumber(basePosition, healAmount, false, DamageType.HEALING, entity.getId(), Vec3.ZERO);
    }

    /**
     * HealNumberMixinから呼ばれる。M&Sスケールの回復値をそのまま表示し、
     * 直後のDamageMixinによる重複を防止するためクールダウンを記録する。
     */
    public void addHealFromMsPacket(LivingEntity entity, float healAmount) {
        Minecraft mc = Minecraft.getInstance();
        int currentTick = mc.player != null ? (int) mc.player.tickCount : 0;
        lastHealEntityId = entity.getId();
        lastHealTick = currentTick;

        Vec3 basePosition = entity.position().add(0, entity.getBbHeight() * config.getPopupHeightRatio(), 0);
        addDamageNumber(basePosition, healAmount, false, DamageType.HEALING, entity.getId(), Vec3.ZERO);
    }

    // ========== Knockback Direction ==========

    /**
     * attacker→entity の方向ベクトルをノックバック初期速度として返す。
     * Y成分は除外し、純粋な水平方向の飛散とする。
     */
    private Vec3 calculateKnockback(LivingEntity entity) {
        if (!(entity.getLastHurtByMob() instanceof LivingEntity)) {
            return Vec3.ZERO;
        }
        LivingEntity attacker = (LivingEntity) entity.getLastHurtByMob();

        Vec3 dir = entity.position().subtract(attacker.position());
        double horizontalLen = Math.sqrt(dir.x * dir.x + dir.z * dir.z);

        if (horizontalLen < 0.01) {
            return Vec3.ZERO;
        }

        double nx = dir.x / horizontalLen;
        double nz = dir.z / horizontalLen;

        return new Vec3(nx * KNOCKBACK_STRENGTH, 0, nz * KNOCKBACK_STRENGTH);
    }

    // ========== Initial Placement (World-Space Spiral) ==========

    /**
     * 同一エンティティのアクティブポップアップが使用中のスロットインデックスを追跡し、
     * 最小の空きスロット（0始まり）を返す。
     * 高さのオフセット計算に使用される。offsetIndexが無制限に増加しないよう上限を設ける。
     */
    private int findAvailableSlot(int entityId) {
        int maxSlots = config.getMaxDamageTexts();
        if (maxSlots <= 0) {
            BitSet used = new BitSet();
            for (DamageNumber dn : damageNumbers) {
                if (dn.getEntityId() == entityId) {
                    int slot = dn.getSlotIndex();
                    if (slot >= 0) {
                        used.set(slot);
                    }
                }
            }
            return used.nextClearBit(0);
        }

        BitSet used = new BitSet(maxSlots);
        for (DamageNumber dn : damageNumbers) {
            if (dn.getEntityId() == entityId) {
                int slot = dn.getSlotIndex();
                if (slot >= 0 && slot < maxSlots) {
                    used.set(slot);
                }
            }
        }

        int free = used.nextClearBit(0);
        return free < maxSlots ? free : maxSlots - 1;
    }

    private Vec3 calculatePositionForSlot(Vec3 basePosition, int slot, DamagePopupMode mode, int entityId) {
        if (mode == DamagePopupMode.VERTICAL_FLOAT) {
            // MOB直上固定。トランプ風スタック重ね合わせは描画時に動的に行う
            return basePosition;
        } else if (mode == DamagePopupMode.POP_ARC) {
            // MOB直上＋ごくわずかな水平ランダム初期位置
            double rx = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;
            double rz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;
            return basePosition.add(rx, 0, rz);
        } else {
            // SPIRAL_SPREAD (デフォルト)
            double angle = slot * 137.508;
            double rad = Math.toRadians(angle);
            int ring = (slot / 8) + 1;
            float radius = SPREAD_RADIUS_BASE + ring * SPREAD_RADIUS_INCREMENT;

            double xOffset = Math.cos(rad) * radius;
            double zOffset = Math.sin(rad) * radius;

            return basePosition.add(xOffset, 0, zOffset);
        }
    }

    // ========== Popup Creation ==========

    public void addDamageNumber(Vec3 position, float damage, boolean isCrit,
                                 DamageType type, int entityId, Vec3 knockback) {
        int max = config.getMaxDamageTexts();
        if (max > 0 && damageNumbers.size() >= max) {
            damageNumbers.remove(0);
        }

        DamagePopupMode mode = config.getPopupMode();
        int slot = findAvailableSlot(entityId);
        Vec3 finalPosition = calculatePositionForSlot(position, slot, mode, entityId);
        damageNumbers.add(new DamageNumber(finalPosition, damage, isCrit, type, entityId, knockback, slot, mode));
    }

    /**
     * 後方互換: 外部Mixinから色指定で呼ばれる旧パス。
     * color引数は無視され、DamageTypeベースでconfigから色が決定される。
     */
    public void addDamageNumber(Vec3 position, float damage, int color, boolean isCrit,
                                DamageType type, int entityId) {
        addDamageNumber(position, damage, isCrit, type, entityId, Vec3.ZERO);
    }

    // ========== Tick & HP Polling ==========

    /**
     * damagenumbers式tickポーリング
     * 毎ティック呼び出され、ポップアップの寿命や物理演算を更新する。LivingEntity.tick()末尾でgetHealth()差分を監視。
     * クライアント同期の有無に関わらず安定してHP変化を検出する。
     * setHealth mixin と重複した場合も、lastHealthChangeTick で除外する。
     */
    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }

        for (DamageNumber dn : damageNumbers) {
            dn.tick();
        }
        damageNumbers.removeIf(DamageNumber::isExpired);

        int currentTick = mc.player.tickCount;

        AABB range = mc.player.getBoundingBox().inflate(POLLING_RANGE);
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, range)) {
            pollHealthChange(entity, currentTick);
        }

        // 定期的に非アクティブエンティティのキャッシュマップをパージ（約10秒毎 = 200ticks）
        cleanupCounter++;
        if (cleanupCounter >= 200) {
            cleanupCounter = 0;
            purgeOldEntityCaches(mc);
        }
    }

    private void purgeOldEntityCaches(Minecraft mc) {
        if (mc.level == null) {
            msDamageHandledTick.clear();
            lastKnownHealth.clear();
            lastHealthChangeTick.clear();
            return;
        }
        int currentTick = mc.player != null ? mc.player.tickCount : 0;
        // 古いTick記録のエントリを削除（200ticks = 10秒以上古いエントリ）
        msDamageHandledTick.entrySet().removeIf(e -> currentTick - e.getValue() > 200);
        lastHealthChangeTick.entrySet().removeIf(e -> currentTick - e.getValue() > 200);
        // ワールドに存在しないエンティティのKnownHealthを削除
        lastKnownHealth.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }

    private void pollHealthChange(LivingEntity entity, int currentTick) {
        int id = entity.getId();
        float currentHealth = entity.getHealth();

        // M&S導入時はバニラルートを完全ブロック。
        // DamageInformationMixin・ヒールMixinのパケット経由で全て処理する。
        if (MethodHandlesUtil.isAvailable()) {
            lastKnownHealth.put(id, currentHealth);
            return;
        }

        if (currentHealth <= 0.01f || entity.isDeadOrDying()) {
            lastKnownHealth.put(id, currentHealth);
            return;
        }

        Float cachedHealth = lastKnownHealth.get(id);
        if (cachedHealth == null) {
            lastKnownHealth.put(id, currentHealth);
            return;
        }

        float diff = cachedHealth - currentHealth;

        if (Math.abs(diff) > 0.1f) {
            Integer lastTick = lastHealthChangeTick.get(id);
            if (lastTick == null || currentTick - lastTick > 2) {
                if (diff > 0) {
                    onDamage(entity, diff);
                } else {
                    onHeal(entity, -diff, true);
                }
                lastHealthChangeTick.put(id, currentTick);
            }
            lastKnownHealth.put(id, currentHealth);
        }
    }

    // ========== Rendering ==========

    public void onRenderWorld(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (!config.isShowDamage()) {
            return;
        }

        if (damageNumbers.isEmpty()) {
            return;
        }

        FontPreset preset = config.getFontPreset();
        ResourceLocation fontLoc = preset.getFontLocation();
        Style fontStyle = fontLoc != null ? Style.EMPTY.withFont(fontLoc) : Style.EMPTY;
        Font font = mc.font;
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.polygonOffset(-1.0f, -1.0f);
        RenderSystem.enablePolygonOffset();

        try {
            // 同一エンティティごとの新旧スタック順位(最新=0, 古い順=1, 2...)を事前計算
            Map<DamageNumber, Integer> entityStackIndices = new HashMap<>();
            Map<Integer, Integer> entityActiveCounts = new HashMap<>();

            for (int i = damageNumbers.size() - 1; i >= 0; i--) {
                DamageNumber dn = damageNumbers.get(i);
                int id = dn.getEntityId();
                int stackIndex = entityActiveCounts.getOrDefault(id, 0);
                entityStackIndices.put(dn, stackIndex);
                entityActiveCounts.put(id, stackIndex + 1);
            }

            int count = damageNumbers.size();
            for (int i = 0; i < count; i++) {
                DamageNumber dn = damageNumbers.get(i);
                int stackIndex = entityStackIndices.getOrDefault(dn, 0);
                renderDamageNumber(poseStack, bufferSource, dn, camPos, font, fontStyle, i, stackIndex);
            }

            bufferSource.endBatch();
        } finally {
            RenderSystem.disablePolygonOffset();
            RenderSystem.polygonOffset(0.0f, 0.0f);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
        }
    }

    private void renderDamageNumber(PoseStack poseStack, MultiBufferSource bufferSource,
                                    DamageNumber dn, Vec3 camPos, Font font, Style fontStyle,
                                    int renderIndex, int stackIndex) {
        poseStack.pushPose();
        try {
            Vec3 pos = dn.getPosition();
            float scale = dn.getScale();

            poseStack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
            poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));

            if (dn.getMode() == DamagePopupMode.VERTICAL_FLOAT) {
                // トランプ風スタック重ね合わせ: 最新(stackIndex=0)が最手前、古いものほど奥(+Z)かつ頭が見えるよう上(-Y)にずらす
                float cardShiftY = -stackIndex * (scale * 4.5f);
                float cardShiftZ = stackIndex * 0.003f;
                poseStack.translate(0, cardShiftY, cardShiftZ);
            } else {
                poseStack.translate(0, 0, -0.002f * renderIndex);
            }

            poseStack.scale(scale, scale, scale);

            int alpha = (int) (dn.getAlpha() * 255);
            int colorWithAlpha = (alpha << 24) | (dn.getDisplayColor() & 0xFFFFFF);

            String text = formatDamageText(dn.getDamage(), dn.getType() == DamageType.HEALING);
            float textWidth = font.width(Component.literal(text).withStyle(fontStyle));
            float x = -textWidth / 2.0f;
            float y = -font.lineHeight / 2.0f;

            if (config.isEnableShadow()) {
                int shadowColor = ((int) (alpha * 0.5f) << 24) | 0x000000;
                DamageFontRenderer.renderText(poseStack, text, x + 1.0f, y + 1.0f,
                    shadowColor, bufferSource, 0xF000F0);
            }

            DamageFontRenderer.renderText(poseStack, text, x, y, colorWithAlpha,
                bufferSource, 0xF000F0);

        } finally {
            poseStack.popPose();
        }
    }

    // ========== Cleanup ==========

    public void clear() {
        damageNumbers.clear();
        msDamageHandledTick.clear();
        lastKnownHealth.clear();
        lastHealthChangeTick.clear();
    }

    // ========== Text Formatting ==========

    private String formatDamageText(float value, boolean isHealing) {
        StringBuilder sb = new StringBuilder(16);

        if (isHealing) {
            sb.append('+');
        }

        float absValue = Math.abs(value);

        if (config.isCompactNumbers()) {
            if (absValue >= 1_000_000f) {
                appendCompact(sb, value, absValue, 1_000_000, 'm');
                return sb.toString();
            } else if (absValue >= 1_000f) {
                appendCompact(sb, value, absValue, 1_000, 'k');
                return sb.toString();
            }
        }

        if (absValue < config.getDecimalThreshold()) {
            appendOneDecimal(sb, value);
        } else {
            sb.append(Math.round(value));
        }

        return sb.toString();
    }

    private void appendCompact(StringBuilder sb, float value, float absValue, int divisor, char suffix) {
        int intPart = (int) (value / divisor);
        int absRemainder = (int) absValue % divisor;
        int dec = absRemainder / (divisor / 10);
        if (dec > 0) {
            sb.append(intPart).append('.').append(dec).append(suffix);
        } else {
            sb.append(intPart).append(suffix);
        }
    }

    private void appendOneDecimal(StringBuilder sb, float value) {
        int intPart = (int) value;
        int decPart = Math.abs((int) ((value - intPart) * 10));
        sb.append(intPart).append('.').append(decPart);
    }
}
