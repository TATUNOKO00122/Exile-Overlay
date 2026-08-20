package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.config.DropSoundConfig;
import com.example.exile_overlay.client.sound.CustomSoundManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ItemEntity に対して M&S レアリティに応じたカスタムドロップ音を再生する Mixin。
 * クライアント側でのみ動作。@Unique フィールドでエンティティ単位の再生状態を管理し重複再生を防止。
 * tickCount <= 5 の間のみ判定し、ワールド読み込み時の既存アイテムでの誤発火を防止。
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    private static EntityDataAccessor<ItemStack> DATA_ITEM;

    @Unique
    private static final Logger EXILE_LOGGER = LoggerFactory.getLogger("exile_overlay/ItemEntityMixin");

    @Unique
    private boolean exileOverlay$hasPlayedDropSound = false;

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * SynchedEntityData の更新時（onSyncedDataUpdated）にフック。
     * サーバーから ItemStack（mmorpg_gear NBT 含む）が同期された瞬間にドロップ音を再生する。
     */
    @Unique
    private static java.util.Set<java.util.UUID> exileOverlay$PLAYED_UUIDS;

    @Unique
    private static java.util.Set<java.util.UUID> exileOverlay$getPlayedUuids() {
        if (exileOverlay$PLAYED_UUIDS == null) {
            exileOverlay$PLAYED_UUIDS = java.util.Collections.newSetFromMap(
                new java.util.LinkedHashMap<java.util.UUID, Boolean>(100, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(java.util.Map.Entry<java.util.UUID, Boolean> eldest) {
                        return size() > 1000;
                    }
                }
            );
        }
        return exileOverlay$PLAYED_UUIDS;
    }

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void exileOverlay$onSyncedDataUpdated(EntityDataAccessor<?> key, CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            return;
        }

        if (!DATA_ITEM.equals(key)) {
            return;
        }

        try {
            ItemStack stack = this.getItem();
            if (stack == null || stack.isEmpty()) {
                return;
            }

            CompoundTag tag = stack.getTag();
            if (tag == null) {
                return;
            }

            if (!tag.contains("mmorpg_gear")) {
                return;
            }

            int entityId = this.getId();
            
            // UI用などのダミーエンティティを除外する (無重力や透明化が設定されていることが多い)
            if (this.isNoGravity() || this.isInvisible() || this.isPassenger()) {
                return;
            }

            String gearJson = tag.getString("mmorpg_gear");
            JsonObject gearObj = JsonParser.parseString(gearJson).getAsJsonObject();
            String rarity = gearObj.has("rar") ? gearObj.get("rar").getAsString() : "unknown";

            if (this.exileOverlay$hasPlayedDropSound) {
                return;
            }

            if (this.tickCount > 5) {
                return;
            }
            
            if (exileOverlay$getPlayedUuids().contains(this.getUUID())) {
                return;
            }

            this.exileOverlay$hasPlayedDropSound = true;

            DropSoundConfig config = DropSoundConfig.getInstance();
            if (!config.isEnabled()) return;

            if (!gearObj.has("rar")) {
                EXILE_LOGGER.debug("[exile_overlay] No 'rar' field in mmorpg_gear JSON: {}", gearJson);
                return;
            }

            DropSoundConfig.RaritySound raritySound = config.getRaritySound(rarity);
            if (raritySound == null || !raritySound.isEnabled()) return;

            ResourceLocation soundLoc = CustomSoundManager.getSafeSoundLocation(raritySound.getSound());
            if (soundLoc == null) return;
            float volume = raritySound.getVolume();
            
            // ItemPhysicLite 等のMODによって即座にItemEntityが置き換えられる(削除される)場合への対策。
            // 処理をメインスレッドのタスクキューの最後に遅延させ、その時点で isRemoved() なら再生しない。
            Minecraft.getInstance().tell(() -> {
                if (this.isRemoved()) {
                    return;
                }
                
                // GUIレンダリング用などのダミーエンティティは、クライアントのワールド(level)に実際には追加されないため
                // ワールド内に自分自身が存在しているかを確認することで、本物のドロップアイテムだけを判別する
                if (this.level().getEntity(this.getId()) != this) {
                    return;
                }
                
                if (exileOverlay$getPlayedUuids().contains(this.getUUID())) {
                    return;
                }
                exileOverlay$getPlayedUuids().add(this.getUUID());

                EXILE_LOGGER.debug("[exile_overlay] Playing drop sound: loc={}, volume={}, entityId={}", soundLoc, volume, entityId);

                // SimpleSoundInstance を直接生成することで、volume > 1.0f のブーストに対応する。
                Minecraft.getInstance().getSoundManager().play(
                        new SimpleSoundInstance(
                                SoundEvent.createVariableRangeEvent(soundLoc).getLocation(),
                                SoundSource.MASTER,
                                volume, 1.0F,
                                RandomSource.create(),
                                false, 0,
                                SoundInstance.Attenuation.NONE,
                                0.0, 0.0, 0.0, true));
            });

        } catch (Exception e) {
            EXILE_LOGGER.error("ItemEntityMixin drop sound error", e);
        }
    }
}
