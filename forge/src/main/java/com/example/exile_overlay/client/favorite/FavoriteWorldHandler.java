package com.example.exile_overlay.client.favorite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.example.exile_overlay.ExampleMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class FavoriteWorldHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteWorldHandler.class);

    @SubscribeEvent
    public static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        String worldId = getWorldIdentifier();
        LOGGER.debug("Client joined world/server: {}", worldId);
        FavoriteItemManager.getInstance().setCurrentWorld(worldId);
    }

    @SubscribeEvent
    public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FavoriteItemManager.getInstance().save();
        FavoriteItemManager.getInstance().clearCurrentWorld();
        LOGGER.debug("Saved and cleared favorites on disconnect");
    }

    public static String getWorldIdentifier() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getCurrentServer() != null) {
            ServerData serverData = mc.getCurrentServer();
            if (serverData.ip != null && !serverData.ip.isEmpty()) {
                return "server_" + sanitizeIdentifier(serverData.ip);
            }
            if (serverData.name != null && !serverData.name.isEmpty()) {
                return "server_" + sanitizeIdentifier(serverData.name);
            }
        }

        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            String worldName = mc.getSingleplayerServer().getWorldPath(null).getFileName().toString();
            if (worldName != null && !worldName.isEmpty()) {
                return "singleplayer_" + sanitizeIdentifier(worldName);
            }
        }

        return "unknown";
    }

    private static String sanitizeIdentifier(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }
}
