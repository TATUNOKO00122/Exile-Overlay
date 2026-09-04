package com.example.exile_overlay.client.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

public class DungeonTimerManager {

    private static final DungeonTimerManager INSTANCE = new DungeonTimerManager();
    private static final String DUNGEON_NAMESPACE = "dungeon_realm";
    private static final String DUNGEON_PATH = "dungeon";

    private static final int DUNGEON_GRID_SIZE = 90;
    private static final int DUNGEON_START_OFFSET = 41;

    private boolean inDungeon = false;
    private boolean active = false;
    private long accumulatedMillis = 0L;
    private long lastEntryTimeMs = 0L;
    private long elapsedSeconds = 0L;
    private String currentInstanceKey = null;

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

        if (nowInDungeon) {
            ChunkPos cp = mc.player.chunkPosition();
            int startX = cp.x + DUNGEON_START_OFFSET - Math.floorMod(cp.x, DUNGEON_GRID_SIZE);
            int startZ = cp.z + DUNGEON_START_OFFSET - Math.floorMod(cp.z, DUNGEON_GRID_SIZE);
            String instanceKey = startX + "_" + startZ;

            if (currentInstanceKey == null || !currentInstanceKey.equals(instanceKey)) {
                currentInstanceKey = instanceKey;
                accumulatedMillis = 0L;
                lastEntryTimeMs = System.currentTimeMillis();
                elapsedSeconds = 0L;
                active = true;
                resetDigits();
            } else if (!inDungeon) {
                lastEntryTimeMs = System.currentTimeMillis();
            }

            inDungeon = true;

            if (active && lastEntryTimeMs > 0L) {
                long totalMillis = accumulatedMillis + (System.currentTimeMillis() - lastEntryTimeMs);
                long currentSec = totalMillis / 1000L;
                if (currentSec != elapsedSeconds) {
                    elapsedSeconds = currentSec;
                    updateDigits(elapsedSeconds);
                }
            }
        } else {
            if (inDungeon) {
                inDungeon = false;
                if (active && lastEntryTimeMs > 0L) {
                    accumulatedMillis += (System.currentTimeMillis() - lastEntryTimeMs);
                    lastEntryTimeMs = 0L;
                }
            }
        }
    }

    public void resetTimer() {
        this.currentInstanceKey = null;
        this.accumulatedMillis = 0L;
        this.lastEntryTimeMs = 0L;
        this.elapsedSeconds = 0L;
        this.active = false;
        this.inDungeon = false;
        resetDigits();
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
