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
            
            // ダメージ合計と属性ごとのダメージを取得
            float totalDamage = 0f;
            String dominantElement = "Physical"; // デフォルト
            float maxElementDamage = 0f;
            
            if (dmgMapObj instanceof Map) {
                Map<?, ?> dmgMap = (Map<?, ?>) dmgMapObj;
                for (Map.Entry<?, ?> entry : dmgMap.entrySet()) {
                    String elementName = entry.getKey().toString();
                    float damage = 0f;
                    if (entry.getValue() instanceof Number) {
                        damage = ((Number) entry.getValue()).floatValue();
                    }
                    totalDamage += damage;
                    
                    // 最もダメージの高い属性を記録
                    if (damage > maxElementDamage) {
                        maxElementDamage = damage;
                        dominantElement = elementName;
                    }
                }
            }
            
            // エンティティがLivingEntityの場合、ダメージポップアップを表示
            if (entity instanceof LivingEntity living && totalDamage > 0) {
                // Playerへのダメージ表示設定をチェック
                if (!DamagePopupConfig.getInstance().isShowPlayerDamage() && living instanceof Player) {
                    ci.cancel(); // Mine and Slashのデフォルト表示もキャンセル
                    return;
                }
                // 属性に応じた色を取得
                int color = getColorForElement(dominantElement);
                
                // 属性に応じたDamageTypeを取得
                DamageType damageType = getDamageTypeForElement(dominantElement);
                
                // クリティカル時は色を変更（オプション：クリティカルの色を優先する場合）
                // int finalColor = isCrit ? 0xFFFF00 : color; // クリティカル優先の場合
                int finalColor = color; // 属性色を優先
                
                // エンティティの頭上に表示（positionは足元なので高さを加算）
                float heightRatio = DamagePopupConfig.getInstance().getPopupHeightRatio();
                var position = living.position().add(0, living.getBbHeight() * heightRatio, 0);
                DamagePopupManager.getInstance().addDamageNumber(
                    position, 
                    totalDamage, 
                    finalColor, 
                    isCrit, 
                    damageType, 
                    living.getId()
                );
                LOGGER.debug("Showing damage popup: {} (crit: {}, element: {}) for entity {}", 
                    totalDamage, isCrit, dominantElement, living.getId());
            }
            
            // Mine and Slashのデフォルト表示をキャンセル
            ci.cancel();
            
        } catch (Exception e) {
            LOGGER.error("Failed to process damage information: {}", e.getMessage(), e);
            // エラー時は元の表示をキャンセルしない（安全のため）
        }
    }
    
    /**
     * 属性名から色を取得
     */
    private int getColorForElement(String element) {
        return switch (element) {
            case "Physical" -> 0xFFFFFF;    // 白色
            case "Fire" -> 0xFF5555;        // 赤色 (M&SのChatFormatting.RED)
            case "Cold" -> 0x55FFFF;        // 水色 (M&SのChatFormatting.AQUA)
            case "Nature" -> 0xFFFF55;      // 黄色 (M&SのChatFormatting.YELLOW)
            case "Shadow" -> 0xAA00AA;      // 紫色 (M&SのChatFormatting.DARK_PURPLE)
            case "Elemental" -> 0xFF55FF;   // 薄紫色
            case "ALL" -> 0xFFFFFF;         // 白色
            default -> 0xFFFFFF;            // デフォルト白色
        };
    }
    
    /**
     * 属性名からDamageTypeを取得
     */
    private DamageType getDamageTypeForElement(String element) {
        return switch (element) {
            case "Fire" -> DamageType.FIRE;
            case "Cold" -> DamageType.ICE;
            case "Nature" -> DamageType.LIGHTNING;
            case "Shadow" -> DamageType.MAGIC;
            default -> DamageType.NORMAL;
        };
    }
}
