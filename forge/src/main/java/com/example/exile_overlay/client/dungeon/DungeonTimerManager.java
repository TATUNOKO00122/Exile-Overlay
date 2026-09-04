package com.example.exile_overlay.client.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class DungeonTimerManager {

    private static final DungeonTimerManager INSTANCE = new DungeonTimerManager();
    private static final String DUNGEON_NAMESPACE = "dungeon_realm";
    private static final String DUNGEON_PATH = "dungeon";

    private boolean inDungeon = false;
    private boolean active = false;
    private long startTimeMs = 0L;
    private long elapsedSeconds = 0L;

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
            inDungeon = false;
            active = false;
            return;
        }

        ResourceLocation dim = mc.level.dimension().location();
        boolean nowInDungeon = DUNGEON_NAMESPACE.equals(dim.getNamespace()) && DUNGEON_PATH.equals(dim.getPath());

        if (!inDungeon && nowInDungeon) {
            inDungeon = true;
            if (!active) {
                startTimeMs = System.currentTimeMillis();
                active = true;
                resetDigits();
            }
        } else if (inDungeon && !nowInDungeon) {
            inDungeon = false;
            active = false;
        }

        if (inDungeon && active) {
            long currentSec = (System.currentTimeMillis() - startTimeMs) / 1000L;
            if (currentSec != elapsedSeconds) {
                elapsedSeconds = currentSec;
                updateDigits(elapsedSeconds);
            }
        }
    }

    public void resetTimer() {
        this.startTimeMs = System.currentTimeMillis();
        this.elapsedSeconds = 0L;
        this.active = true;
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
