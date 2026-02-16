package com.example.exile_overlay.forge.mixin;

import com.example.exile_overlay.client.damage.DamagePopupManager;
import com.example.exile_overlay.client.damage.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mixin(targets = "com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.IParticleSpawnMaterial$DamageInformation", remap = false)
public class DamageInformationMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageInformationMixin");

    @Inject(method = "spawnOnClient", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$cancelDamageParticleSpawn(Entity entity, CallbackInfo ci) {
        try {
            Object self = (Object) this;
            
            // isCrit() getter メソッドを呼び出し
            Method isCritMethod = self.getClass().getDeclaredMethod("isCrit");
            isCritMethod.setAccessible(true);
            boolean isCrit = (boolean) isCritMethod.invoke(self);
            
            // getDmgMap() メソッドを呼び出し
            Method getDmgMapMethod = self.getClass().getDeclaredMethod("getDmgMap");
            getDmgMapMethod.setAccessible(true);
            Object dmgMapObj = getDmgMapMethod.invoke(self);
            
            // ダメージ合計を計算
            float totalDamage = 0f;
            if (dmgMapObj instanceof Map) {
                Map<?, ?> dmgMap = (Map<?, ?>) dmgMapObj;
                for (Object value : dmgMap.values()) {
                    if (value instanceof Number) {
                        totalDamage += ((Number) value).floatValue();
                    }
                }
            }
            
            // エンティティがLivingEntityの場合、ダメージポップアップを表示
            if (entity instanceof LivingEntity living && totalDamage > 0) {
                int color = isCrit ? 0xFFFF0000 : 0xFFFFFFFF; // クリティカル:赤、通常:白
                // エンティティの頭上に表示（positionは足元なので高さを加算）
                var position = living.position().add(0, living.getBbHeight() * 1.2, 0);
                DamagePopupManager.getInstance().addDamageNumber(
                    position, 
                    totalDamage, 
                    color, 
                    isCrit, 
                    DamageType.NORMAL, 
                    living.getId()
                );
                LOGGER.debug("Showing damage popup: {} (crit: {}) for entity {}", totalDamage, isCrit, living.getId());
            }
            
            // Mine and Slashのデフォルト表示をキャンセル
            ci.cancel();
            
        } catch (Exception e) {
            LOGGER.error("Failed to process damage information: {}", e.getMessage(), e);
            // エラー時は元の表示をキャンセルしない（安全のため）
        }
    }
}
