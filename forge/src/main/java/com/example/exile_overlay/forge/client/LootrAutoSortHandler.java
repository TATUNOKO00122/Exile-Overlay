package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.util.InventorySorterHelper;
import com.example.exile_overlay.util.LootrHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class LootrAutoSortHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/LootrAutoSort");
    private static final int SORT_DELAY_TICKS = 5;
    
    private static final Set<String> LOOTR_BLOCKS = Set.of(
        "lootr:lootr_chest",
        "lootr:lootr_trapped_chest",
        "lootr:lootr_barrel",
        "lootr:lootr_shulker"
    );
    
    private static BlockPos cachedTargetPos = null;
    private static int tickCounter = 0;
    private static boolean waitingToSort = false;
    
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!LootrHelper.isLoaded()) return;
        if (!InventorySorterHelper.isAvailable()) return;
        
        EquipmentDisplayConfig config = EquipmentDisplayConfig.getInstance();
        if (!config.isAutoSortLootrChest()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        
        BlockPos pos = blockHit.getBlockPos();
        
        BlockState state = mc.level.getBlockState(pos);
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        
        if (LOOTR_BLOCKS.contains(blockId)) {
            cachedTargetPos = pos;
            tickCounter = 0;
            waitingToSort = true;
            LOGGER.debug("Scheduled auto-sort for Lootr container: {} at {}", blockId, pos);
        }
    }
    
    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        cachedTargetPos = null;
        waitingToSort = false;
        tickCounter = 0;
    }
    
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!waitingToSort) return;
        if (!InventorySorterHelper.isAvailable()) return;
        
        tickCounter++;
        
        if (tickCounter >= SORT_DELAY_TICKS) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen != null) {
                InventorySorterHelper.sortCurrentContainer();
                LOGGER.info("Auto-sorted Lootr container");
            }
            
            waitingToSort = false;
            cachedTargetPos = null;
            tickCounter = 0;
        }
    }
}