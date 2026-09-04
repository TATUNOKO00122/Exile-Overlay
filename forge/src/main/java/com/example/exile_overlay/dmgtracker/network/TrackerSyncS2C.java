package com.example.exile_overlay.dmgtracker.network;

import com.example.exile_overlay.dmgtracker.tracking.SkillDamageStats;
import com.example.exile_overlay.dmgtracker.tracking.PlayerTrackerData;
import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import com.example.exile_overlay.dmgtracker.tracking.TimestampedDamage;
import com.example.exile_overlay.dmgtracker.util.SkillIdResolver;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;

public class TrackerSyncS2C {
    private final List<SkillStatsEntry> entries;
    private final double totalDamage;
    private final int totalHits;
    private final int totalKills;
    private final float overallDps;
    private final long sessionSeconds;
    private final boolean inCombat;

    public TrackerSyncS2C(PlayerTrackerData data) {
        this.entries = new ArrayList<>();
        for (SkillDamageStats s : data.getTopSkillsByDamage(20)) {
            Map<String, Double> elemMap = new HashMap<>();
            for (Map.Entry<Elements, Double> e : s.getDamageByElement().entrySet()) {
                elemMap.put(e.getKey().name(), e.getValue());
            }
            entries.add(new SkillStatsEntry(
                    s.getSkillId(), s.getDisplayName(), SkillIdResolver.extractRawSpellId(s.getSkillId()),
                    s.getTotalDamage(), s.getHitCount(), s.getCritCount(),
                    s.getMissCount(), s.getMaxHit(), s.getMinHit(), s.getKillCount(), s.getDps(),
                    s.getCritRate(), elemMap, s.getDominantElement()
            ));
        }
        this.totalDamage = data.getTotalDamage();
        this.totalHits = data.getTotalHits();
        this.totalKills = data.getTotalKills();
        this.overallDps = data.getOverallDps();
        this.sessionSeconds = data.getSessionDurationSeconds();
        this.inCombat = data.isInCombat();
    }

    private TrackerSyncS2C(List<SkillStatsEntry> entries, double totalDamage, int totalHits,
                           int totalKills, float overallDps, long sessionSeconds, boolean inCombat) {
        this.entries = entries;
        this.totalDamage = totalDamage;
        this.totalHits = totalHits;
        this.totalKills = totalKills;
        this.overallDps = overallDps;
        this.sessionSeconds = sessionSeconds;
        this.inCombat = inCombat;
    }

    public static void sendToPlayer(ServerPlayer player) {
        PlayerTrackerData data = DamageTrackerManager.getTracker(player.getUUID());
        TrackerSyncS2C packet = new TrackerSyncS2C(data);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(totalDamage);
        buf.writeVarInt(totalHits);
        buf.writeVarInt(totalKills);
        buf.writeFloat(overallDps);
        buf.writeLong(sessionSeconds);
        buf.writeBoolean(inCombat);
        buf.writeVarInt(entries.size());
        for (SkillStatsEntry e : entries) {
            buf.writeUtf(e.skillId);
            buf.writeUtf(e.displayName);
            buf.writeUtf(e.rawSpellId);
            buf.writeDouble(e.totalDamage);
            buf.writeVarInt(e.hitCount);
            buf.writeVarInt(e.critCount);
            buf.writeVarInt(e.missCount);
            buf.writeFloat(e.maxHit);
            buf.writeFloat(e.minHit);
            buf.writeVarInt(e.killCount);
            buf.writeFloat(e.dps);
            buf.writeFloat(e.critRate);
            buf.writeUtf(e.dominantElement);
            buf.writeVarInt(e.elementDamage.size());
            for (Map.Entry<String, Double> el : e.elementDamage.entrySet()) {
                buf.writeUtf(el.getKey());
                buf.writeDouble(el.getValue());
            }
        }
    }

    public static TrackerSyncS2C decode(FriendlyByteBuf buf) {
        double totalDamage = buf.readDouble();
        int totalHits = buf.readVarInt();
        int totalKills = buf.readVarInt();
        float overallDps = buf.readFloat();
        long sessionSeconds = buf.readLong();
        boolean inCombat = buf.readBoolean();
        int size = buf.readVarInt();
        List<SkillStatsEntry> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String skillId = buf.readUtf();
            String displayName = buf.readUtf();
            String rawSpellId = buf.readUtf();
            double dmg = buf.readDouble();
            int hits = buf.readVarInt();
            int crits = buf.readVarInt();
            int misses = buf.readVarInt();
            float maxHit = buf.readFloat();
            float minHit = buf.readFloat();
            int kills = buf.readVarInt();
            float dps = buf.readFloat();
            float critRate = buf.readFloat();
            String dominantElement = buf.readUtf();
            int elemSize = buf.readVarInt();
            Map<String, Double> elemMap = new HashMap<>();
            for (int j = 0; j < elemSize; j++) {
                elemMap.put(buf.readUtf(), buf.readDouble());
            }
            entries.add(new SkillStatsEntry(skillId, displayName, rawSpellId, dmg, hits, crits, misses,
                    maxHit, minHit, kills, dps, critRate, elemMap, dominantElement));
        }
        return new TrackerSyncS2C(entries, totalDamage, totalHits, totalKills, overallDps, sessionSeconds, inCombat);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() != net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT) return;
        ctx.get().enqueueWork(() -> {
            ClientTrackerData.set(this);
        });
        ctx.get().setPacketHandled(true);
    }

    public List<SkillStatsEntry> getEntries() { return entries; }
    public double getTotalDamage() { return totalDamage; }
    public int getTotalHits() { return totalHits; }
    public int getTotalKills() { return totalKills; }
    public float getOverallDps() { return overallDps; }
    public long getSessionSeconds() { return sessionSeconds; }
    public boolean isInCombat() { return inCombat; }

    public static class SkillStatsEntry {
        public final String skillId;
        public final String displayName;
        public final String rawSpellId;
        public final double totalDamage;
        public final int hitCount;
        public final int critCount;
        public final int missCount;
        public final float maxHit;
        public final float minHit;
        public final int killCount;
        public final float dps;
        public final float critRate;
        public final Map<String, Double> elementDamage;
        public final String dominantElement;

        public SkillStatsEntry(String skillId, String displayName, String rawSpellId, double totalDamage, int hitCount,
                               int critCount, int missCount, float maxHit, float minHit, int killCount,
                               float dps, float critRate, Map<String, Double> elementDamage,
                               String dominantElement) {
            this.skillId = skillId;
            this.displayName = displayName;
            this.rawSpellId = rawSpellId != null ? rawSpellId : "";
            this.totalDamage = totalDamage;
            this.hitCount = hitCount;
            this.critCount = critCount;
            this.missCount = missCount;
            this.maxHit = maxHit;
            this.minHit = minHit;
            this.killCount = killCount;
            this.dps = dps;
            this.critRate = critRate;
            this.elementDamage = elementDamage;
            this.dominantElement = dominantElement;
        }
    }

    public static class ClientTrackerData {
        private static TrackerSyncS2C lastData;
        private static long receivedAtMs = 0;
        private static boolean serverHasMod = false;

        public static void set(TrackerSyncS2C data) {
            lastData = data;
            receivedAtMs = System.currentTimeMillis();
            serverHasMod = true;
        }

        public static TrackerSyncS2C get() {
            return lastData;
        }

        public static long getReceivedAtMs() {
            return receivedAtMs;
        }

        public static boolean serverHasMod() {
            return serverHasMod;
        }

        public static void resetServerPresence() {
            serverHasMod = false;
            lastData = null;
        }

        public static float getLiveOverallDps() {
            if (lastData == null) return 0;
            if (!lastData.isInCombat()) return 0;
            return lastData.getOverallDps();
        }
    }
}
