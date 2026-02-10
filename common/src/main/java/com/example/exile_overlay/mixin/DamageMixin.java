package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.damage.DamageType;
import com.example.exile_overlay.client.damage.ThreadSafeDamageRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public abstract class DamageMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageMixin");

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract float getMaxHealth();

    @Unique
    private float exileOverlay$lastHealth = -1;

    @Unique
    private DamageSource exileOverlay$lastDamageSource = null;

    @Unique
    private long exileOverlay$lastDamageTime = 0;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void exileOverlay$onHurtStart(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!((LivingEntity) (Object) this).level().isClientSide()) {
                return;
            }
            
            // 防御的なnullチェック
            if (source == null) {
                LOGGER.debug("Null damage source received, skipping tracking");
                return;
            }
            
            // ダメージソースを記録
            exileOverlay$lastDamageSource = source;
            exileOverlay$lastDamageTime = System.currentTimeMillis();
        } catch (Exception e) {
            LOGGER.error("Failed to track damage start", e);
            // 例外をキャッチしてゲームを継続
        }
    }

    @Inject(method = "setHealth", at = @At("TAIL"))
    private void exileOverlay$onSetHealth(float health, CallbackInfo ci) {
        try {
            LivingEntity entity = (LivingEntity) (Object) this;

            // クライアント側のみで処理
            if (!entity.level().isClientSide()) {
                return;
            }

            // プレイヤー自身のダメージは表示しない（オプション）
            if (entity instanceof Player && entity == Minecraft.getInstance().player) {
                exileOverlay$lastHealth = health;
                return;
            }

            // 初期化時はスキップ
            if (exileOverlay$lastHealth < 0) {
                exileOverlay$lastHealth = health;
                return;
            }

            // ダメージを計算
            float damage = exileOverlay$lastHealth - health;
            if (damage > 0.1f) {
                // ダメージを表示
                Vec3 position = entity.position().add(0, entity.getBbHeight() * 1.2, 0);
                int color = exileOverlay$getColorForDamageSource(exileOverlay$lastDamageSource);
                boolean isCrit = false;

                // クリティカル判定（直近100ms以内のダメージソースがあれば使用）
                if (exileOverlay$lastDamageSource != null && 
                    System.currentTimeMillis() - exileOverlay$lastDamageTime < 100) {
                    if (exileOverlay$lastDamageSource.getEntity() instanceof Player) {
                        Player attacker = (Player) exileOverlay$lastDamageSource.getEntity();
                        isCrit = attacker.getAttackStrengthScale(0.5f) > 0.9f;
                    }
                }

                LOGGER.debug("Damage detected: {} to {}", damage, entity.getType().getDescription().getString());

                ThreadSafeDamageRenderer.getInstance().addDamageNumber(
                        position,
                        damage,
                        color,
                        isCrit,
                        DamageType.NORMAL,
                        entity.getId()
                );
            }

            exileOverlay$lastHealth = health;
        } catch (Exception e) {
            LOGGER.error("Failed to display damage popup", e);
            // 例外をキャッチしてゲームを継続
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void exileOverlay$onTick(CallbackInfo ci) {
        try {
            LivingEntity entity = (LivingEntity) (Object) this;

            if (!entity.level().isClientSide()) {
                return;
            }

            // ヘルスの初期化
            if (exileOverlay$lastHealth < 0) {
                exileOverlay$lastHealth = entity.getHealth();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize health tracking", e);
        }
    }

    @Unique
    private int exileOverlay$getColorForDamageSource(DamageSource source) {
        if (source == null) {
            return 0xFFFFFF;
        }

        String msgId = source.getMsgId();

        if (msgId.contains("fire") || msgId.contains("burn") || msgId.contains("lava") || msgId.contains("hotFloor")) {
            return 0xFF6600;
        } else if (msgId.contains("magic")) {
            return 0x800080;
        } else if (msgId.contains("explosion")) {
            return 0xFF4500;
        } else if (msgId.contains("wither")) {
            return 0x2F2F2F;
        } else if (msgId.contains("poison")) {
            return 0x00FF00;
        } else if (msgId.contains("lightning")) {
            return 0xFFFF66;
        } else if (msgId.contains("freeze")) {
            return 0x00CCFF;
        } else {
            return 0xFFFFFF;
        }
    }
}