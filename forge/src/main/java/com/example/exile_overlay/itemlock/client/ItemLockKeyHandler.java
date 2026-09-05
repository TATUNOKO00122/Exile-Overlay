package com.example.exile_overlay.itemlock.client;

import com.example.exile_overlay.client.event.ExileOverlayForgeClient;
import com.example.exile_overlay.dmgtracker.network.NetworkHandler;
import com.example.exile_overlay.itemlock.ItemLockHelper;
import com.example.exile_overlay.itemlock.LockManager;
import com.example.exile_overlay.itemlock.network.LockSlotC2S;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * アイテムロックの入力操作ハンドラ。
 * 設定キー+クリックによるロック切り替え、およびロック中スロットに対する各種操作（クリック/Shift/Qドロップ/数字キー）をブロックする。
 */
public final class ItemLockKeyHandler {

    private ItemLockKeyHandler() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ItemLockKeyHandler());
    }

    /**
     * ロック切り替え用修飾キーが押されているか判定
     */
    public static boolean isToggleKeyDown() {
        KeyMapping key = ExileOverlayForgeClient.toggleItemLockKey;
        if (key == null || key.isUnbound()) {
            return false;
        }
        int keyCode = key.getKey().getValue();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getKey().getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, keyCode);
        } else if (key.getKey().getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, keyCode) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    /**
     * コンテナ画面でのマウス操作インターセプト
     */
    @SubscribeEvent
    public void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        Slot slot = containerScreen.getSlotUnderMouse();
        int slotIdx = ItemLockHelper.getPlayerSlotIndex(slot, mc.player);
        if (slotIdx < 0) return;

        // 設定キー + 左クリック: ロック状態の切り替え
        if (event.getButton() == 0 && isToggleKeyDown()) {
            event.setCanceled(true);
            LockManager.toggleClientSlotLock(slotIdx);
            String storageKey = ItemLockClientStorage.getCurrentStorageKey();
            if (storageKey != null) {
                ItemLockClientStorage.setLockMask(storageKey, LockManager.getClientLockedMask());
            }
            if (ItemLockClientStorage.isServerModPresent()) {
                NetworkHandler.CHANNEL.sendToServer(new LockSlotC2S(slotIdx));
            }
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
            return;
        }

        // 通常操作時: ロックされたスロットへの直接クリックやShift移動を遮断
        if (LockManager.isClientSlotLocked(slotIdx)) {
            event.setCanceled(true);
        }
    }

    /**
     * コンテナ画面でのキーボード操作インターセプト（Qキードロップ、数字キー入れ替え、オフハンドFキー入れ替え）
     */
    @SubscribeEvent
    public void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        Slot slot = containerScreen.getSlotUnderMouse();
        int slotIdx = ItemLockHelper.getPlayerSlotIndex(slot, mc.player);

        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();

        // 1. Qキーによるドロップ防止
        if (mc.options.keyDrop.matches(keyCode, scanCode)) {
            if (slotIdx >= 0 && LockManager.isClientSlotLocked(slotIdx)) {
                event.setCanceled(true);
                return;
            }
        }

        // 2. ホットバー数字キー（1〜9）での入れ替え防止
        for (int i = 0; i < mc.options.keyHotbarSlots.length; i++) {
            if (mc.options.keyHotbarSlots[i].matches(keyCode, scanCode)) {
                // ホバー中スロットまたは入れ替え先スロットがロックされている場合は禁止
                if ((slotIdx >= 0 && LockManager.isClientSlotLocked(slotIdx)) || LockManager.isClientSlotLocked(i)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        // 3. オフハンドキー（Fキー）での入れ替え防止
        if (mc.options.keySwapOffhand.matches(keyCode, scanCode)) {
            if (slotIdx >= 0 && LockManager.isClientSlotLocked(slotIdx)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * 通常画面（インベントリを閉じた状態）でのQキードロップおよびオフハンドFキー入れ替え防止
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int selected = mc.player.getInventory().selected;
        if (!LockManager.isClientSlotLocked(selected)) return;

        // 1. ロックされた選択中スロットのQキードロップ防止
        if (mc.options.keyDrop.matches(event.getKey(), event.getScanCode())) {
            while (mc.options.keyDrop.consumeClick()) {
                // クリックキューを空にする
            }
        }

        // 2. ロックされた選択中スロットのオフハンド（Fキー）入れ替え防止
        if (mc.options.keySwapOffhand.matches(event.getKey(), event.getScanCode())) {
            while (mc.options.keySwapOffhand.consumeClick()) {
                // クリックキューを空にする
            }
        }
    }

    /**
     * ワールド参加時に接続先に応じたロック状態を復元し、サーバーにMODが存在する場合のみ同期を要求
     */
    @SubscribeEvent
    public void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        String storageKey = ItemLockClientStorage.getCurrentStorageKey();
        if (storageKey != null) {
            long savedMask = ItemLockClientStorage.getLockMask(storageKey);
            LockManager.setClientLockedMask(savedMask);
        }
        if (ItemLockClientStorage.isServerModPresent()) {
            NetworkHandler.CHANNEL.sendToServer(new LockSlotC2S(LockSlotC2S.SYNC_REQUEST_SLOT));
        }
    }

    /**
     * ワールド退出時に現在の接続先スコープにキャッシュを保存してリセット
     */
    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        String storageKey = ItemLockClientStorage.getCurrentStorageKey();
        if (storageKey != null) {
            ItemLockClientStorage.setLockMask(storageKey, LockManager.getClientLockedMask());
        }
        LockManager.resetClient();
    }
}
