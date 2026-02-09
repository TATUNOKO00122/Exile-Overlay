package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.MineAndSlashDataProvider;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * オーブのレジストリ
 * ここに登録されたオーブが描画されます
 */
public class OrbRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<OrbType> registeredOrbs = new ArrayList<>();
    private static boolean initialized = false;

    /**
     * レジストリを初期化する
     * Mod初期化時に一度だけ呼び出してください
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.debug("OrbRegistry already initialized, skipping");
            return;
        }

        LOGGER.info("Initializing OrbRegistry");

        // MineAndSlashデータプロバイダーを登録
        ModDataProviderRegistry.register(new MineAndSlashDataProvider());

        // デフォルトのオーブを登録
        registerDefaultOrbs();

        initialized = true;
        LOGGER.info("OrbRegistry initialized with {} orbs", registeredOrbs.size());
    }

    /**
     * デフォルトのオーブを登録する
     */
    private static void registerDefaultOrbs() {
        // デフォルトで登録されるオーブ
        register(OrbType.HEALTH);
        register(OrbType.MAGIC_SHIELD);
        register(OrbType.ENERGY);
        register(OrbType.MANA);
        register(OrbType.BLOOD);
    }

    /**
     * オーブを登録する
     * 新しいオーブを追加する場合はこのメソッドを使用してください
     *
     * @param orbType 登録するオーブ
     */
    public static void register(OrbType orbType) {
        if (orbType == null) {
            LOGGER.warn("Attempted to register null orb type");
            return;
        }

        if (!registeredOrbs.contains(orbType)) {
            registeredOrbs.add(orbType);
            LOGGER.debug("Registered orb: {}", orbType.getId());
        } else {
            LOGGER.debug("Orb {} is already registered, skipping", orbType.getId());
        }
    }

    /**
     * オーブの登録を解除する
     *
     * @param orbType 解除するオーブ
     */
    public static void unregister(OrbType orbType) {
        if (registeredOrbs.remove(orbType)) {
            LOGGER.debug("Unregistered orb: {}", orbType.getId());
        }
    }

    /**
     * IDでオーブを取得する
     *
     * @param id オーブのID
     * @return 見つかったオーブ、またはnull
     */
    public static OrbType getById(String id) {
        for (OrbType orb : registeredOrbs) {
            if (orb.getId().equals(id)) {
                return orb;
            }
        }
        return null;
    }

    /**
     * 登録されている全てのオーブを取得（変更不可リスト）
     *
     * @return 登録済みオーブのリスト
     */
    public static List<OrbType> getRegisteredOrbs() {
        return Collections.unmodifiableList(new ArrayList<>(registeredOrbs));
    }

    /**
     * プレイヤーに表示すべきオーブを取得
     *
     * @param player 対象プレイヤー
     * @return 表示可能なオーブのリスト
     */
    public static List<OrbType> getVisibleOrbs(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }

        List<OrbType> visible = new ArrayList<>();
        for (OrbType orb : registeredOrbs) {
            try {
                if (orb.getConfig().isVisible(player)) {
                    visible.add(orb);
                }
            } catch (Exception e) {
                LOGGER.debug("Error checking visibility for orb {}: {}", orb.getId(), e.getMessage());
            }
        }
        return visible;
    }

    /**
     * レジストリをクリア（テスト用）
     */
    public static void clear() {
        registeredOrbs.clear();
        initialized = false;
        LOGGER.info("OrbRegistry cleared");
    }

    /**
     * 初期化済みかどうか
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
