package com.example.exile_overlay.client.event;

import com.example.exile_overlay.ExileOverlayMod;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.util.InventorySorterHelper;
import com.example.exile_overlay.util.LootrHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = ExileOverlayMod.MOD_ID, value = Dist.CLIENT)
public class LootrAutoSortHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/LootrAutoSort");

    private static boolean waitingToSort = false;
    private static BlockPos lastLootrClickPos = null;
    private static Integer lastLootrEntityId = null;
    private static long lastLootrClickTime = 0;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        if (LootrHelper.isLootrContainerAt(event.getLevel(), event.getPos())) {
            lastLootrClickPos = event.getPos();
            lastLootrEntityId = null;
            lastLootrClickTime = System.currentTimeMillis();
            LOGGER.debug("[AutoSort] Clicked Lootr block at pos: {}", event.getPos());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        if (LootrHelper.isLootrEntity(event.getTarget())) {
            lastLootrClickPos = event.getTarget().blockPosition();
            lastLootrEntityId = event.getTarget().getId();
            lastLootrClickTime = System.currentTimeMillis();
            LOGGER.debug("[AutoSort] Clicked Lootr entity {} at {}", event.getTarget().getType(), lastLootrClickPos);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        waitingToSort = false;
        if (!LootrHelper.isLoaded() || !InventorySorterHelper.isAvailable()) return;

        EquipmentDisplayConfig config = EquipmentDisplayConfig.getInstance();
        if (!config.isAutoSortLootrChest()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Ensure the screen is a container screen (not pause menu, etc.)
        if (!(event.getScreen() instanceof AbstractContainerScreen)) {
            return;
        }

        // Prevent sorting player inventory, crafting tables, furnaces, or modded backpacks
        if (LootrHelper.isExcludedScreen(event.getScreen())) {
            return;
        }

        if (lastLootrClickPos == null || System.currentTimeMillis() - lastLootrClickTime >= 2000) {
            return;
        }

        BlockPos pos = lastLootrClickPos;
        Integer entityId = lastLootrEntityId;
        lastLootrClickPos = null; // consume
        lastLootrEntityId = null;
        lastLootrClickTime = 0;

        // 対象の実体（Block または Entity）が依然として Lootr であることを再検証（誤爆防止）
        boolean verified = false;
        if (entityId != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null && entity.isAlive() && LootrHelper.isLootrEntity(entity)) {
                verified = true;
            }
        }
        if (!verified && LootrHelper.isLootrContainerAt(mc.level, pos)) {
            verified = true;
        }

        if (!verified) {
            LOGGER.debug("[AutoSort] Target validation failed at pos {}", pos);
            return;
        }

        waitingToSort = true;
        LOGGER.debug("[AutoSort] SCHEDULED after right click at {}", pos);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        waitingToSort = false;
        lastLootrClickPos = null;
        lastLootrEntityId = null;
        lastLootrClickTime = 0;
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!waitingToSort) return;
        waitingToSort = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen != null) {
            InventorySorterHelper.sortCurrentContainer();
            LOGGER.info("[AutoSort] EXECUTED");
        }
    }
}
