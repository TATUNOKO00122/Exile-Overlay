package com.example.exile_overlay.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class BossPortalMarkerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BossPortalMarkerManager.class);
    private static final BossPortalMarkerManager INSTANCE = new BossPortalMarkerManager();

    private final Set<BlockPos> markedPortals = new HashSet<>();
    private BlockPos lastPlayerPos = null;
    private ResourceLocation lastDimension = null;
    private long lastScanTime = 0;
    
    private static final long SCAN_INTERVAL_MS = 1000; // 1秒おきにスキャン
    private static final double TP_THRESHOLD_SQ = 32.0 * 32.0; // 32ブロック以上の移動をTPとみなす
    private static final double NEAR_PORTAL_DIST_SQ = 5.0 * 5.0; // ポータルの近く（5ブロック）

    private BossPortalMarkerManager() {}

    public static BossPortalMarkerManager getInstance() {
        return INSTANCE;
    }

    public void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        // 設定が有効かどうかをチェック
        if (!com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableBossPortalMarker()) {
            if (!markedPortals.isEmpty()) {
                LOGGER.info("exile_overlay: Boss portal marker is disabled in config. Clearing markers.");
                clearMarkers();
            }
            reset();
            return;
        }

        Player player = mc.player;
        Level level = mc.level;
        BlockPos currentPos = player.blockPosition();
        ResourceLocation currentDim = level.dimension().location();

        // 1. ディメンション切り替えチェック
        if (lastDimension == null || !lastDimension.equals(currentDim)) {
            LOGGER.info("exile_overlay: Dimension changed to {}. Clearing markers.", currentDim);
            clearMarkers();
            lastDimension = currentDim;
            lastPlayerPos = currentPos;
            return;
        }

        // 2. テレポート検出（急激な長距離移動）
        if (lastPlayerPos != null) {
            double distSq = currentPos.distSqr(lastPlayerPos);
            if (distSq >= TP_THRESHOLD_SQ) {
                LOGGER.info("exile_overlay: Teleport detected (dist={}). Checking markers.", Math.sqrt(distSq));
                // 移動前に、いずれかのマーカーポータルの近くにいたか？
                boolean wasNearPortal = false;
                for (BlockPos portalPos : markedPortals) {
                    if (lastPlayerPos.distSqr(portalPos) <= NEAR_PORTAL_DIST_SQ) {
                        wasNearPortal = true;
                        break;
                    }
                }
                if (wasNearPortal) {
                    LOGGER.info("exile_overlay: Player teleported from near a portal. Clearing markers.");
                    clearMarkers();
                    lastPlayerPos = currentPos;
                    return;
                }
            }
        }
        lastPlayerPos = currentPos;

        // 3. 周囲のスキャン（1秒間隔）
        long now = System.currentTimeMillis();
        if (now - lastScanTime >= SCAN_INTERVAL_MS) {
            lastScanTime = now;
            scanAroundPlayer(player, level, currentPos);
        }
    }

    private void scanAroundPlayer(Player player, Level level, BlockPos center) {
        // 水平半径24ブロック、垂直半径12ブロックの範囲をスキャン
        int rH = 24;
        int rV = 12;
        
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
        for (int x = -rH; x <= rH; x++) {
            for (int y = -rV; y <= rV; y++) {
                for (int z = -rH; z <= rH; z++) {
                    mutPos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    
                    // クライアント側でその座標がロードされているか確認
                    if (!level.hasChunkAt(mutPos)) continue;
                    
                    BlockState state = level.getBlockState(mutPos);
                    Block block = state.getBlock();
                    ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
                    
                    if (blockId != null) {
                        String idStr = blockId.toString();
                        // dungeon_realm の boss_teleport と uber_teleport を対象とする
                        if ("dungeon_realm:boss_teleport".equals(idStr) || "dungeon_realm:uber_teleport".equals(idStr)) {
                            BlockPos immutablePos = mutPos.immutable();
                            if (!markedPortals.contains(immutablePos)) {
                                markedPortals.add(immutablePos);
                                String name = "dungeon_realm:boss_teleport".equals(idStr) ? "Boss Portal" : "Uber Boss Portal";
                                LOGGER.info("exile_overlay: Found portal: {} at {}", name, immutablePos);
                                XaeroWaypointHelper.addWaypoint(player, immutablePos, name, "Ω", 14);
                            }
                        }
                    }
                }
            }
        }
    }

    public void clearMarkers() {
        if (!markedPortals.isEmpty()) {
            markedPortals.clear();
        }
        XaeroWaypointHelper.clearAllWaypoints();
    }

    public void reset() {
        markedPortals.clear();
        lastPlayerPos = null;
        lastDimension = null;
    }
}
