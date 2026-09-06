package com.example.exile_overlay.dmgtracker.network;

import com.example.exile_overlay.client.render.ailment.ClientAilmentTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * エンティティの状態異常（Ailment）同期パケット（S2C）
 */
public class AilmentSyncS2C {

    private final int entityId;
    private final List<AilmentEntry> ailments;

    public record AilmentEntry(
            String id,
            int ticksLeft,
            int stacks,
            float strength,
            float damage
    ) {}

    public AilmentSyncS2C(int entityId, List<AilmentEntry> ailments) {
        this.entityId = entityId;
        this.ailments = ailments != null ? ailments : new ArrayList<>();
    }

    public int getEntityId() {
        return entityId;
    }

    public List<AilmentEntry> getAilments() {
        return ailments;
    }

    public static void encode(AilmentSyncS2C msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeByte(msg.ailments.size());
        for (AilmentEntry entry : msg.ailments) {
            buf.writeUtf(entry.id());
            buf.writeVarInt(entry.ticksLeft());
            buf.writeVarInt(entry.stacks());
            buf.writeFloat(entry.strength());
            buf.writeFloat(entry.damage());
        }
    }

    public static AilmentSyncS2C decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int size = buf.readByte();
        List<AilmentEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            int ticksLeft = buf.readVarInt();
            int stacks = buf.readVarInt();
            float strength = buf.readFloat();
            float damage = buf.readFloat();
            list.add(new AilmentEntry(id, ticksLeft, stacks, strength, damage));
        }
        return new AilmentSyncS2C(entityId, list);
    }

    public static void handle(AilmentSyncS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection() != net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT) return;
        ctx.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> ClientPayloadHandler.handle(msg.entityId, msg.ailments));
        });
        ctx.setPacketHandled(true);
    }

    private static final class ClientPayloadHandler {
        private static void handle(int entityId, List<AilmentEntry> ailments) {
            ClientAilmentTracker.getInstance().handleSync(entityId, ailments);
        }
    }

    public static void sendToTracking(Entity entity, AilmentSyncS2C packet) {
        if (entity == null || packet == null || entity.level().isClientSide()) return;
        try {
            NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        } catch (Exception ignored) {
        }
    }
}
