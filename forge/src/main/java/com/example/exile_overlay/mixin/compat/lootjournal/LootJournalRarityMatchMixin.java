package com.example.exile_overlay.mixin.compat.lootjournal;

import com.example.exile_overlay.client.config.LootJournalCompatConfig;
import com.example.exile_overlay.util.ItemRarityResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Pseudo
@Mixin(targets = "dev.obscuria.lootjournal.client.themes.match.RarityMatch", remap = false)
public abstract class LootJournalRarityMatchMixin {

    @Shadow
    private String value;

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true, remap = false)
    private void exileOverlay$matchMsRarity(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            LootJournalCompatConfig config = LootJournalCompatConfig.getInstance();
            if (!config.isEnableCompat()) {
                return;
            }

            String msRarity = ItemRarityResolver.resolveRarity(stack);
            if (msRarity != null && this.value != null) {
                String target = this.value.toLowerCase(Locale.ROOT);
                String actual = msRarity.toLowerCase(Locale.ROOT);

                // uncommon_item と uncommon の同一視
                if (actual.equals("uncommon_item")) {
                    actual = "uncommon";
                }

                if (actual.equals(target)) {
                    cir.setReturnValue(true);
                    return;
                }

                // 上位レアリティ（mythic, legendary, unique）の場合、
                // Loot Journalの既存テーマにスタイル定義が存在しないため、epic スタイル（光彩演出など）へ自然にフォールバック
                if (target.equals("epic")) {
                    if (actual.equals("mythic") || actual.equals("legendary") || actual.equals("unique")) {
                        cir.setReturnValue(true);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
