package com.example.exile_overlay.mixin;

import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.IParticleSpawnMaterial;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Pseudo
@Mixin(targets = "com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.ExileInteractionResultPacket", remap = false)
public class ExileInteractionResultPacketMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ExileInteractionResultPacketMixin");

    @Shadow
    public int id;

    @Shadow
    private IParticleSpawnMaterial notifier;

    @Inject(method = "onReceived", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onPacketReceived(ExilePacketContext exilePacketContext, CallbackInfo ci) {
        try {
            if (this.notifier != null && exilePacketContext != null && exilePacketContext.getPlayer() != null) {
                Entity entity = exilePacketContext.getPlayer().level().getEntity(this.id);
                this.notifier.spawnOnClient(entity);
                ci.cancel();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process ExileInteractionResultPacket: {}", e.getMessage(), e);
        }
    }
}
