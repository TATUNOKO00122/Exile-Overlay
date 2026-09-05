package com.example.exile_overlay.client.util;

import com.example.exile_overlay.api.data.MobEffectInfo;
import com.example.exile_overlay.client.render.ailment.ClientAilmentTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 物理クライアント環境でのみロードされるシングルプレイ用ヘルパー。
 * Dedicated Server 環境での NoClassDefFoundError を防止する。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSingleplayerHelper {

    private ClientSingleplayerHelper() {}

    public static LivingEntity resolveServerEntity(LivingEntity entity) {
        if (entity == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            try {
                ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(entity.level().dimension());
                if (serverLevel != null) {
                    Entity serverEntity = serverLevel.getEntity(entity.getUUID());
                    if (serverEntity instanceof LivingEntity living) {
                        return living;
                    }
                }
            } catch (Throwable ignore) {}
        }
        return null;
    }

    public static List<MobEffectInfo> getAilmentEffects(LivingEntity entity) {
        return ClientAilmentTracker.getInstance().getAilmentEffects(entity);
    }

    public static ResourceLocation resolveElementIcon(String ailmentId) {
        return ClientAilmentTracker.resolveElementIcon(ailmentId);
    }
}
