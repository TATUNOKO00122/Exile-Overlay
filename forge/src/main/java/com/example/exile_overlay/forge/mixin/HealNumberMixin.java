package com.example.exile_overlay.forge.mixin;

import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.damage.DamagePopupManager;
import com.example.exile_overlay.client.damage.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
@Mixin(targets = "com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.IParticleSpawnMaterial$HealNumber", remap = false)
public class HealNumberMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/HealNumberMixin");

    @Inject(method = "spawnOnClient", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$cancelHealParticleSpawn(Entity entity, CallbackInfo ci) {
        try {
            Object self = (Object) this;
            
            // number() getter メソッドを呼び出し
            Method numberMethod = self.getClass().getDeclaredMethod("number");
            numberMethod.setAccessible(true);
            float healAmount = ((Number) numberMethod.invoke(self)).floatValue();
            
            if (entity instanceof LivingEntity living && healAmount > 0) {
                DamagePopupConfig config = DamagePopupConfig.getInstance();
                
                // 回復表示設定をチェック
                if (!config.isShowHealing()) {
                    ci.cancel();
                    return;
                }
                
                // Playerへの回復表示設定をチェック
                if (living instanceof Player && !config.isShowPlayerHealing()) {
                    ci.cancel();
                    return;
                }
                
                // エンティティの頭上に表示（positionは足元なので高さを加算）
                var position = living.position().add(0, living.getBbHeight() * config.getPopupHeightRatio(), 0);
                DamagePopupManager.getInstance().addDamageNumber(
                    position,
                    healAmount,
                    0xFF00FF00, // 緑色
                    false,
                    DamageType.HEALING,
                    living.getId()
                );
                LOGGER.debug("Showing heal popup: {} for entity {}", healAmount, living.getId());
            }
            
            // Mine and Slashのデフォルト表示をキャンセル
            ci.cancel();
            
        } catch (Exception e) {
            LOGGER.error("Failed to process heal number: {}", e.getMessage(), e);
        }
    }
}
