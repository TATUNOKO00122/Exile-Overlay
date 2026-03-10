package com.example.exile_overlay.client.favorite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.architectury.platform.Platform;
import net.minecraft.world.entity.player.Inventory;
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

/**
 * Manages favorite items in the player's inventory.
 * Favorite items are protected from all operations (quick move, drop, drag, etc.)
 * 
 * Player inventory slots:
 * - 0-8: Hotbar
 * - 9-35: Main inventory
 * - 36-39: Armor (boots, leggings, chestplate, helmet)
 * - 40: Offhand
 * 
 * CLIENT-ONLY - Do not use on server side.
 */
public class FavoriteItemManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteItemManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static FavoriteItemManager INSTANCE;
    
    // Map: WorldName -> Set of favorite slot IDs (player inventory slots: 0-40)
    private final Map<String, Set<Integer>> favoritesByWorld = new HashMap<>();
    private String currentWorld = "";

    private FavoriteItemManager() {
        load();
    }

    /**
     * Get singleton instance.
     * This should only be called from client-side code.
     */
    public static FavoriteItemManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FavoriteItemManager();
        }
        return INSTANCE;
    }

    /**
     * Set the current world name for data isolation.
     */
    public void setCurrentWorld(String worldName) {
        this.currentWorld = worldName != null ? worldName : "";
        load();
    }

    /**
     * Get the current world name
     */
    public String getCurrentWorld() {
        return currentWorld;
    }

    /**
     * Get the current world's favorites set.
     */
    private Set<Integer> getFavorites() {
        return favoritesByWorld.computeIfAbsent(currentWorld, k -> new HashSet<>());
    }

    /**
     * Toggle favorite status for a slot.
     * 
     * @param slotId The slot ID in player inventory (0-40 for main inventory + hotbar + armor + offhand)
     * @return true if now favorite, false if removed from favorites
     */
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

    /**
     * Check if a slot is marked as favorite.
     * 
     * @param slotId The slot ID in player inventory
     * @return true if the slot is favorite
     */
    public boolean isFavorite(int slotId) {
        return getFavorites().contains(slotId);
    }

    /**
     * Check if a slot is favorite by container slot index.
     * Container screens typically have container slots first, then player inventory.
     * Player inventory usually starts at containerSize - 36.
     * 
     * @param containerSlotIndex The slot index in the container menu
     * @param playerInvStart     The starting index of player inventory in the container
     * @return true if this slot corresponds to a favorite player inventory slot
     */
    public boolean isFavoriteContainerSlot(int containerSlotIndex, int playerInvStart) {
        if (containerSlotIndex < playerInvStart) {
            return false; // Not a player inventory slot
        }
        int playerSlotId = containerSlotIndex - playerInvStart;
        return isFavorite(playerSlotId);
    }

    /**
     * Convert a slot to player inventory slot ID.
     * Returns -1 if not a player inventory slot.
     * 
     * @param slot The slot to check
     * @return Player inventory slot ID (0-40) or -1 if not player inventory
     */
    public int toPlayerSlotId(Slot slot) {
        if (slot == null) {
            return -1;
        }
        if (slot.container instanceof Inventory) {
            // getContainerSlot() returns the actual inventory index (0-40)
            // slot.index is the container menu position which varies by screen type
            return slot.getContainerSlot();
        }
        return -1;
    }

    /**
     * Get absolute path to the favorites data file.
     */
    private Path getDataPath() {
        return Platform.getConfigFolder()
                .resolve("exile_overlay")
                .resolve("favorites.json");
    }

    /**
     * Load favorites from disk.
     */
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

    /**
     * Save favorites to disk.
     */
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

    /**
     * Clear all favorites for the current world.
     */
    public void clearAll() {
        getFavorites().clear();
        save();
        LOGGER.debug("Cleared all favorites for world: {}", currentWorld);
    }

    /**
     * Get count of favorites for current world
     */
    public int getFavoriteCount() {
        return getFavorites().size();
    }

    /**
     * Check if there are any favorites in current world
     */
    public boolean hasFavorites() {
        return !getFavorites().isEmpty();
    }
}
