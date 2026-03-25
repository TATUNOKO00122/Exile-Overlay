package com.example.exile_overlay.client.favorite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FavoriteItemManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteItemManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static FavoriteItemManager INSTANCE;
    
    private final Map<String, Set<Integer>> favoritesByWorld = new HashMap<>();
    private String currentWorld = "";

    private FavoriteItemManager() {
        load();
    }

    public static FavoriteItemManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FavoriteItemManager();
        }
        return INSTANCE;
    }

    public void setCurrentWorld(String worldName) {
        this.currentWorld = worldName != null ? worldName : "";
        load();
    }

    public String getCurrentWorld() {
        return currentWorld;
    }

    private Set<Integer> getFavorites() {
        return favoritesByWorld.computeIfAbsent(currentWorld, k -> new HashSet<>());
    }

    public boolean toggleFavorite(int slotId) {
        Set<Integer> favorites = getFavorites();
        if (favorites.contains(slotId)) {
            favorites.remove(slotId);
            save();
            LOGGER.debug("Removed favorite from slot {}", slotId);
            return false;
        } else {
            favorites.add(slotId);
            save();
            LOGGER.debug("Added favorite to slot {}", slotId);
            return true;
        }
    }

    public boolean isFavorite(int slotId) {
        return getFavorites().contains(slotId);
    }

    public int toPlayerSlotId(Slot slot, Inventory playerInventory) {
        return InventorySlotHelper.toPlayerSlotId(slot, playerInventory);
    }

    public boolean isFavorite(Slot slot, Inventory playerInventory) {
        int playerSlotId = toPlayerSlotId(slot, playerInventory);
        if (playerSlotId < 0) {
            return false;
        }
        return isFavorite(playerSlotId);
    }

    public boolean isBypassActive() {
        return FavoriteKeyBindings.isBypass();
    }

    public boolean isToggleKeyPressed() {
        return FavoriteKeyBindings.isToggle();
    }

    private Path getDataPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve("exile_overlay")
                .resolve("favorites.json");
    }

    public void load() {
        Path path = getDataPath();
        if (!Files.exists(path)) {
            LOGGER.debug("Favorites file not found at {}", path);
            return;
        }

        try {
            String json = Files.readString(path);
            Map<String, Set<Integer>> loaded = GSON.fromJson(json,
                    new TypeToken<Map<String, Set<Integer>>>() {}.getType());
            if (loaded != null) {
                favoritesByWorld.clear();
                favoritesByWorld.putAll(loaded);
                LOGGER.debug("Loaded favorites for {} world(s)", favoritesByWorld.size());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load favorites", e);
        }
    }

    public void save() {
        Path path = getDataPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(favoritesByWorld));
            LOGGER.debug("Saved favorites to {}", path);
        } catch (IOException e) {
            LOGGER.error("Failed to save favorites", e);
        }
    }

    public void clearAll() {
        getFavorites().clear();
        save();
        LOGGER.debug("Cleared all favorites for world: {}", currentWorld);
    }

    public int getFavoriteCount() {
        return getFavorites().size();
    }

    public boolean hasFavorites() {
        return !getFavorites().isEmpty();
    }
}