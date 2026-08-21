package com.example.exile_overlay.client.config.screen.entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * 設定画面のリストに表示される各項目の基底クラス。
 */
public abstract class ConfigEntry {

    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final Font font = Minecraft.getInstance().font;

    /**
     * エントリを描画する
     *
     * @param guiGraphics 描画用コンテキスト
     * @param x           描画開始X座標（リスト項目の左端）
     * @param y           描画開始Y座標（リスト項目の上端）
     * @param width       項目の幅
     * @param height      項目の高さ
     * @param mouseX      マウスX座標
     * @param mouseY      マウスY座標
     * @param isHovered   項目全体がホバーされているか
     * @param partialTick フレーム補間時間
     */
    public abstract void render(GuiGraphics guiGraphics, int x, int y, int width, int height,
                               int mouseX, int mouseY, boolean isHovered, float partialTick);

    /**
     * @return エントリの表示高さ（デフォルト24px）
     */
    public int getHeight() {
        return 24;
    }

    /**
     * 検索クエリに一致するかどうかを判定する
     *
     * @param query 小文字に変換された検索クエリ
     * @return 一致する場合はtrue
     */
    public abstract boolean matchesSearch(String query);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    /**
     * 文字列またはComponentが検索クエリを含むかを判定するヘルパー
     */
    protected static boolean textMatches(Component component, String query) {
        if (component == null || query == null || query.isEmpty()) {
            return false;
        }
        return component.getString().toLowerCase(Locale.ROOT).contains(query);
    }

    protected static boolean textMatches(String text, String query) {
        if (text == null || query == null || query.isEmpty()) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(query);
    }
}
