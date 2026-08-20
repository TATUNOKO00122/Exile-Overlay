package com.example.exile_overlay.client.event;

import com.example.exile_overlay.ExileOverlayMod;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExileOverlayMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExileOverlayGui {
    private static final EquipmentDisplayConfig EQUIP_CONFIG = EquipmentDisplayConfig.getInstance();
    private static final HudPositionManager POSITION_MANAGER = HudPositionManager.getInstance();

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        boolean hotbarVisible = POSITION_MANAGER.getPosition("hotbar").isVisible();
        boolean vanillaFoodVisible = POSITION_MANAGER.getPosition("vanilla_food").isVisible();
        boolean vanillaAirVisible = POSITION_MANAGER.getPosition("vanilla_air").isVisible();
        boolean skillHotbarVisible = POSITION_MANAGER.getPosition("skill_hotbar").isVisible();
        boolean buffVisible = POSITION_MANAGER.getPosition("buff_overlay").isVisible();
        boolean skillBuffVisible = POSITION_MANAGER.getPosition("skill_buff_overlay").isVisible();

        if (hotbarVisible && (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() ||
                event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type() ||
                event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() ||
                event.getOverlay() == VanillaGuiOverlay.HOTBAR.type())) {
            event.setCanceled(true);
            return;
        }

        if (vanillaFoodVisible && event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
            return;
        }

        if (vanillaAirVisible && event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
            event.setCanceled(true);
            return;
        }

        if (event.getOverlay() == VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            if (POSITION_MANAGER.getPosition("boss_hp_bar").isVisible()) {
                event.setCanceled(true);
            }
            return;
        }

        if (event.getOverlay() == VanillaGuiOverlay.JUMP_BAR.type()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof Boat) {
                event.setCanceled(true);
                return;
            }
        }

        String overlayId = event.getOverlay().id().toString();
        if (overlayId.contains("botania") && overlayId.contains("mana") && EQUIP_CONFIG.isCancelBotaniaMana()) {
            if (POSITION_MANAGER.getPosition("botania_mana_bar").isVisible()) {
                event.setCanceled(true);
                return;
            }
        }
        if (overlayId.equals("irons_spellbooks:mana_overlay")) {
            if (hotbarVisible) {
                event.setCanceled(true);
                return;
            }
        }
        if (overlayId.endsWith("mmorpg.rpg_gui") && EQUIP_CONFIG.isCancelMnsRpgBars()) {
            if (hotbarVisible) {
                event.setCanceled(true);
                return;
            }
        }
        if (overlayId.endsWith("mmorpg.spell_hotbar") && EQUIP_CONFIG.isCancelMnsSpellHotbar()) {
            if (skillHotbarVisible) {
                event.setCanceled(true);
                return;
            }
        }
        if (overlayId.endsWith("mmorpg.cast_bar") && EQUIP_CONFIG.isCancelMnsCastBar()) {
            if (hotbarVisible || skillHotbarVisible) {
                event.setCanceled(true);
                return;
            }
        }
        if (overlayId.endsWith("mmorpg.status_effects") && EQUIP_CONFIG.isCancelMnsStatusEffects()) {
            if (buffVisible || skillBuffVisible) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
