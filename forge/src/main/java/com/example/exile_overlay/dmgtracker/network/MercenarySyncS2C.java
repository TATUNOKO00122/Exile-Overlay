package com.example.exile_overlay.dmgtracker.network;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.api.data.MercenaryDisplayInfo;
import com.example.exile_overlay.api.data.MercenarySkillInfo;
import com.example.exile_overlay.client.render.minion.MercenaryClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 傭兵のステータス・スキルクールダウン同期パケット（S2C）
 */
public class MercenarySyncS2C {

    private final boolean hasMercenary;
    private final String classId;
    private final String name;
    private final int level;
    private final float health;
    private final float maxHealth;
    private final float energyShield;
    private final float maxEnergyShield;
    private final List<SkillData> skills;

    public record SkillData(
            String spellId,
            boolean onCooldown,
            float progress,
            int remainingTicks,
            int totalTicks
    ) {}

    public MercenarySyncS2C() {
        this.hasMercenary = false;
        this.classId = "";
        this.name = "";
        this.level = 1;
        this.health = 0;
        this.maxHealth = 0;
        this.energyShield = 0;
        this.maxEnergyShield = 0;
        this.skills = new ArrayList<>();
    }

    public MercenarySyncS2C(String classId, String name, int level,
                           float health, float maxHealth,
                           float energyShield, float maxEnergyShield,
                           List<SkillData> skills) {
        this.hasMercenary = true;
        this.classId = classId != null ? classId : "";
        this.name = name != null ? name : "Mercenary";
        this.level = Math.max(1, level);
        this.health = health;
        this.maxHealth = maxHealth;
        this.energyShield = energyShield;
        this.maxEnergyShield = maxEnergyShield;
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public boolean hasMercenary() {
        return hasMercenary;
    }

    public static void encode(MercenarySyncS2C msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasMercenary);
        if (msg.hasMercenary) {
            buf.writeUtf(msg.classId, 128);
            buf.writeUtf(msg.name, 128);
            buf.writeInt(msg.level);
            buf.writeFloat(msg.health);
            buf.writeFloat(msg.maxHealth);
            buf.writeFloat(msg.energyShield);
            buf.writeFloat(msg.maxEnergyShield);
            buf.writeInt(msg.skills.size());
            for (SkillData skill : msg.skills) {
                buf.writeUtf(skill.spellId(), 128);
                buf.writeBoolean(skill.onCooldown());
                buf.writeFloat(skill.progress());
                buf.writeInt(skill.remainingTicks());
                buf.writeInt(skill.totalTicks());
            }
        }
    }

    public static MercenarySyncS2C decode(FriendlyByteBuf buf) {
        boolean hasMerc = buf.readBoolean();
        if (!hasMerc) {
            return new MercenarySyncS2C();
        }
        String classId = buf.readUtf(128);
        String name = buf.readUtf(128);
        int level = buf.readInt();
        float health = buf.readFloat();
        float maxHealth = buf.readFloat();
        float energyShield = buf.readFloat();
        float maxEnergyShield = buf.readFloat();
        int skillCount = buf.readInt();
        List<SkillData> skills = new ArrayList<>(skillCount);
        for (int i = 0; i < skillCount; i++) {
            String spellId = buf.readUtf(128);
            boolean onCd = buf.readBoolean();
            float progress = buf.readFloat();
            int remaining = buf.readInt();
            int total = buf.readInt();
            skills.add(new SkillData(spellId, onCd, progress, remaining, total));
        }
        return new MercenarySyncS2C(classId, name, level, health, maxHealth, energyShield, maxEnergyShield, skills);
    }

    public String getClassId() { return classId; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getEnergyShield() { return energyShield; }
    public float getMaxEnergyShield() { return maxEnergyShield; }
    public List<SkillData> getSkills() { return skills; }

    public static void handle(MercenarySyncS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection() != net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT) return;
        ctx.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> ClientPayloadHandler.handle(msg));
        });
        ctx.setPacketHandled(true);
    }

    private static final class ClientPayloadHandler {
        private static void handle(MercenarySyncS2C msg) {
            if (!msg.hasMercenary) {
                MercenaryClientCache.clear();
                return;
            }

            ResourceLocation classIcon = MethodHandlesUtil.getMercClassIconById(msg.classId);

            List<MercenarySkillInfo> skillInfos = new ArrayList<>();
            for (SkillData s : msg.skills) {
                ResourceLocation spellIcon = MethodHandlesUtil.getSpellIconByGuid(s.spellId());
                skillInfos.add(new MercenarySkillInfo(
                        s.spellId(),
                        spellIcon,
                        s.onCooldown(),
                        s.progress(),
                        s.remainingTicks(),
                        s.totalTicks()
                ));
            }

            MercenaryDisplayInfo displayInfo = new MercenaryDisplayInfo(
                    msg.classId,
                    msg.name,
                    classIcon,
                    msg.level,
                    msg.health,
                    msg.maxHealth,
                    msg.energyShield,
                    msg.maxEnergyShield,
                    skillInfos
            );

            MercenaryClientCache.update(displayInfo);
        }
    }

    public static void sendToPlayer(ServerPlayer player, MercenarySyncS2C packet) {
        if (player == null || packet == null) return;
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
