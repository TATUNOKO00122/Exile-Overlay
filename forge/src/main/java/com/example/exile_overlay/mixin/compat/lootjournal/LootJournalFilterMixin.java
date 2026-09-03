package com.example.exile_overlay.mixin.compat.lootjournal;

import com.example.exile_overlay.client.config.LootJournalCompatConfig;
import com.example.exile_overlay.util.DropItemResolver;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.obscuria.lootjournal.LootJournal", remap = false)
public abstract class LootJournalFilterMixin {

    @Inject(method = "isAllowed", at = @At("HEAD"), cancellable = true, remap = false)
    private static void exileOverlay$filterOnlyMsItems(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            LootJournalCompatConfig config = LootJournalCompatConfig.getInstance();
            if (config.isEnableCompat() && config.isOnlyMsItems()) {
                if (!DropItemResolver.isMsItem(stack)) {
                    cir.setReturnValue(false);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
