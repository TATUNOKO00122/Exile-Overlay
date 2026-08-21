package com.example.exile_overlay.client.config.screen.list;

import com.example.exile_overlay.client.config.screen.entry.ConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 設定エントリの一覧をスクロール表示・検索フィルタリングするリストコンポーネント。
 */
public class ConfigEntryList {

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Font font = Minecraft.getInstance().font;

    private int x;
    private int y;
    private int width;
    private int height;

    private final List<ConfigEntry> allEntries = new ArrayList<>();
    private final List<ConfigEntry> visibleEntries = new ArrayList<>();

    private double scrollOffset = 0;
    private int maxScroll = 0;
    private int totalContentHeight = 0;
    private boolean isDraggingScrollbar = false;
    private String currentFilter = "";

    public ConfigEntryList(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        updateLayout();
    }

    public void setEntries(List<ConfigEntry> entries) {
        this.allEntries.clear();
        if (entries != null) {
            this.allEntries.addAll(entries);
        }
        applyFilter();
    }

    public void setFilter(String filter) {
        this.currentFilter = (filter != null) ? filter.trim().toLowerCase(Locale.ROOT) : "";
        applyFilter();
    }

    public void resetScroll() {
        this.scrollOffset = 0;
        clampScroll();
    }

    private void applyFilter() {
        this.visibleEntries.clear();
        if (currentFilter.isEmpty()) {
            this.visibleEntries.addAll(allEntries);
        } else {
            for (ConfigEntry entry : allEntries) {
                if (entry.matchesSearch(currentFilter)) {
                    this.visibleEntries.add(entry);
                }
            }
        }
        updateLayout();
    }

    public void updateLayout() {
        totalContentHeight = 0;
        for (ConfigEntry entry : visibleEntries) {
            totalContentHeight += entry.getHeight();
        }
        totalContentHeight += 10; // 下部余白

        maxScroll = Math.max(0, totalContentHeight - height);
        clampScroll();
    }

    private void clampScroll() {
        if (scrollOffset < 0) {
            scrollOffset = 0;
        } else if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景パネル
        guiGraphics.fill(x, y, x + width, y + height, 0x44000000);

        if (visibleEntries.isEmpty()) {
            if (!currentFilter.isEmpty()) {
                Component noResult = Component.translatable("exile_overlay.config.search.no_results");
                int textX = x + (width - font.width(noResult)) / 2;
                int textY = y + (height - 9) / 2;
                guiGraphics.drawString(font, noResult, textX, textY, 0x88AAAAAA);
            }
            return;
        }

        guiGraphics.enableScissor(x, y, x + width, y + height);

        int currentY = y + 5 - (int) scrollOffset;
        for (ConfigEntry entry : visibleEntries) {
            int entryH = entry.getHeight();
            if (currentY + entryH >= y && currentY <= y + height) {
                boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY < currentY + entryH;
                entry.render(guiGraphics, x, currentY, width - (maxScroll > 0 ? 8 : 0), entryH, mouseX, mouseY, isHovered, partialTick);
            }
            currentY += entryH;
        }

        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            renderScrollBar(guiGraphics);
        }
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        int scrollBarWidth = 6;
        int scrollBarX = x + width - scrollBarWidth - 2;
        int scrollBarHeight = Math.max(20, (int) ((double) height / totalContentHeight * height));
        int scrollBarY = y + (int) ((double) scrollOffset / maxScroll * (height - scrollBarHeight));

        guiGraphics.fill(scrollBarX, y, scrollBarX + scrollBarWidth, y + height, 0x66000000);
        guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, scrollBarY + scrollBarHeight,
                isDraggingScrollbar ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isMouseOver(mouseX, mouseY) && maxScroll > 0) {
            scrollOffset -= delta * 20;
            clampScroll();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        // スクロールバードラッグ開始判定
        if (button == 0 && maxScroll > 0) {
            int scrollBarWidth = 6;
            int scrollBarX = x + width - scrollBarWidth - 2;
            if (mouseX >= scrollBarX - 2 && mouseX <= scrollBarX + scrollBarWidth + 2) {
                isDraggingScrollbar = true;
                return true;
            }
        }

        // 各エントリへクリック伝播
        int currentY = y + 5 - (int) scrollOffset;
        for (ConfigEntry entry : visibleEntries) {
            int entryH = entry.getHeight();
            if (mouseY >= currentY && mouseY < currentY + entryH) {
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            currentY += entryH;
        }

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }

        for (ConfigEntry entry : visibleEntries) {
            if (entry.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && maxScroll > 0) {
            int scrollBarHeight = Math.max(20, (int) ((double) height / totalContentHeight * height));
            double scrollFactor = (double) maxScroll / (height - scrollBarHeight);
            scrollOffset += dragY * scrollFactor;
            clampScroll();
            return true;
        }

        for (ConfigEntry entry : visibleEntries) {
            if (entry.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ConfigEntry entry : visibleEntries) {
            if (entry.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        for (ConfigEntry entry : visibleEntries) {
            if (entry.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
