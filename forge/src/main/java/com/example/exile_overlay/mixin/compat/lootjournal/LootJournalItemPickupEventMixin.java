package com.example.exile_overlay.mixin.compat.lootjournal;

import com.example.exile_overlay.client.config.LootJournalCompatConfig;
import com.example.exile_overlay.util.DropItemResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.obscuria.lootjournal.client.events.ItemPickupEvent", remap = false)
public abstract class LootJournalItemPickupEventMixin {

    @Shadow
    private ItemStack stack;

    @Shadow
    private Component displayName;

    @Inject(method = "updateDisplayName", at = @At("TAIL"), remap = false)
    private void exileOverlay$applyMsDisplayName(CallbackInfo ci) {
        try {
            LootJournalCompatConfig config = LootJournalCompatConfig.getInstance();
            if (config.isEnableCompat() && DropItemResolver.isMsItem(this.stack)) {
                Component fullDisplayName = DropItemResolver.resolveDisplayName(this.stack, config.isShowFullAffixName());
                if (fullDisplayName != null && !fullDisplayName.getString().isEmpty()) {
                    this.displayName = fullDisplayName;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
