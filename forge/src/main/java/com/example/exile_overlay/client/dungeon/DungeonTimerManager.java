package com.example.exile_overlay.client.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class DungeonTimerManager {

    private static final DungeonTimerManager INSTANCE = new DungeonTimerManager();
    private static final String DUNGEON_NAMESPACE = "dungeon_realm";
    private static final String DUNGEON_PATH = "dungeon";

    private boolean inDungeon = false;
    private boolean active = false;
    private long accumulatedMillis = 0L;
    private long lastEntryTimeMs = 0L;
    private long elapsedSeconds = 0L;
    private String currentSessionKey = null;

    private final RollingDigit hourTens = new RollingDigit();
    private final RollingDigit hourOnes = new RollingDigit();
    private final RollingDigit minTens = new RollingDigit();
    private final RollingDigit minOnes = new RollingDigit();
    private final RollingDigit secTens = new RollingDigit();
    private final RollingDigit secOnes = new RollingDigit();

    private DungeonTimerManager() {
    }

    public static DungeonTimerManager getInstance() {
        return INSTANCE;
    }

    public void onClientTick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            if (inDungeon) {
                if (active && lastEntryTimeMs > 0L) {
                    accumulatedMillis += (System.currentTimeMillis() - lastEntryTimeMs);
                    lastEntryTimeMs = 0L;
                }
                inDungeon = false;
            }
            return;
        }

        ResourceLocation dim = mc.level.dimension().location();
        boolean nowInDungeon = DUNGEON_NAMESPACE.equals(dim.getNamespace()) && DUNGEON_PATH.equals(dim.getPath());

        if (!inDungeon && nowInDungeon) {
            inDungeon = true;
            lastEntryTimeMs = System.currentTimeMillis();
            if (!active) {
                active = true;
                accumulatedMillis = 0L;
                resetDigits();
            }
        } else if (inDungeon && !nowInDungeon) {
            inDungeon = false;
            if (active && lastEntryTimeMs > 0L) {
                accumulatedMillis += (System.currentTimeMillis() - lastEntryTimeMs);
                lastEntryTimeMs = 0L;
            }
        }

        if (inDungeon && active && lastEntryTimeMs > 0L) {
            long totalMillis = accumulatedMillis + (System.currentTimeMillis() - lastEntryTimeMs);
            long currentSec = totalMillis / 1000L;
            if (currentSec != elapsedSeconds) {
                elapsedSeconds = currentSec;
                updateDigits(elapsedSeconds);
            }
        }
    }

    public void onMapSnapshotReceived(ItemStack snapshotStack) {
        String newKey = extractSessionKey(snapshotStack);
        if (newKey == null) {
            return;
        }

        if (currentSessionKey != null && currentSessionKey.equals(newKey)) {
            return;
        }

        startNewSession(newKey);
    }

    public void startNewSession(String sessionKey) {
        this.currentSessionKey = sessionKey;
        this.accumulatedMillis = 0L;
        this.lastEntryTimeMs = inDungeon ? System.currentTimeMillis() : 0L;
        this.elapsedSeconds = 0L;
        this.active = true;
        resetDigits();
    }

    public void resetTimer() {
        this.currentSessionKey = null;
        this.accumulatedMillis = 0L;
        this.lastEntryTimeMs = 0L;
        this.elapsedSeconds = 0L;
        this.active = false;
        this.inDungeon = false;
        resetDigits();
    }

    public static String extractSessionKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("dungeon_realm_dungeon_map", Tag.TAG_COMPOUND)) {
                CompoundTag mapTag = tag.getCompound("dungeon_realm_dungeon_map");
                int x = mapTag.getInt("x");
                int z = mapTag.getInt("z");
                if (x != 0 || z != 0) {
                    return x + "_" + z;
                }
            }
            return String.valueOf(tag.hashCode());
        }
        return stack.getItem().toString();
    }

    private void resetDigits() {
        hourTens.reset(0);
        hourOnes.reset(0);
        minTens.reset(0);
        minOnes.reset(0);
        secTens.reset(0);
        secOnes.reset(0);
    }

    private void updateDigits(long totalSeconds) {
        int hours = (int) (totalSeconds / 3600L);
        int minutes = (int) ((totalSeconds % 3600L) / 60L);
        int seconds = (int) (totalSeconds % 60L);

        if (hours > 0) {
            hourTens.setDigit(Math.min(hours / 10, 9));
            hourOnes.setDigit(hours % 10);
        }
        minTens.setDigit(Math.min(minutes / 10, 9));
        minOnes.setDigit(minutes % 10);
        secTens.setDigit(seconds / 10);
        secOnes.setDigit(seconds % 10);
    }

    public boolean isInDungeon() {
        return inDungeon;
    }

    public boolean isActive() {
        return active;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public RollingDigit getHourTens() {
        return hourTens;
    }

    public RollingDigit getHourOnes() {
        return hourOnes;
    }

    public RollingDigit getMinTens() {
        return minTens;
    }

    public RollingDigit getMinOnes() {
        return minOnes;
    }

    public RollingDigit getSecTens() {
        return secTens;
    }

    public RollingDigit getSecOnes() {
        return secOnes;
    }
}
