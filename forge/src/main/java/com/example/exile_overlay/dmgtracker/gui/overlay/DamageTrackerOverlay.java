package com.example.exile_overlay.dmgtracker.gui.overlay;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.example.exile_overlay.dmgtracker.config.TrackerConfig;
import com.example.exile_overlay.dmgtracker.gui.ElementColors;
import com.example.exile_overlay.dmgtracker.gui.FormatUtil;
import com.example.exile_overlay.dmgtracker.network.TrackerSyncS2C;
import com.mojang.blaze3d.systems.RenderSystem;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class DamageTrackerOverlay implements IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamageTrackerOverlay.class);
    private static final String COMMAND_ID = "damage_tracker";
    private static final int PRIORITY = 70;

    private static final int MIN_WIDTH = 180;
    private static final int NAME_VAL_GAP = 20;

    private static final int PADDING = 4;
    private static final int MARGIN = 4;
    private static final int ROW_H = 14;
    private static final int ROW_GAP = 1;
    private static final int ICON_S = 14;
    private static final int BAR_ALPHA = 160;
    private static final int BG_COLOR = 0xB0101010;
    private static final int HEADER_TEXT = 0xFFFFFFFF;
    private static final int ACCENT_COLOR = 0xFF7ECFA0;
    private static final int VALUE_COLOR = 0xFFC8C8C8;

    private static final Map<String, Float> animRatio = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Boolean> VALID_ICON_CACHE = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return COMMAND_ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.TEXT;
    }

    @Override
    public String getConfigKey() {
        return "damage_tracker";
    }

    @Override
    public int getWidth() {
        return MIN_WIDTH;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public int getConfigWidth() {
        Minecraft mc = Minecraft.getInstance();
        TrackerSyncS2C data = TrackerSyncS2C.ClientTrackerData.get();
        if (data == null || data.getEntries().isEmpty()) {
            return MIN_WIDTH;
        }
        List<TrackerSyncS2C.SkillStatsEntry> rows = data.getEntries();
        int count = Math.min(rows.size(), TrackerConfig.getMaxSkillsShown());
        return calcBoxWidth(mc, count, rows, data.getTotalDamage());
    }

    @Override
    public int getConfigHeight() {
        Minecraft mc = Minecraft.getInstance();
        TrackerSyncS2C data = TrackerSyncS2C.ClientTrackerData.get();
        if (data == null || data.getEntries().isEmpty()) {
            return 80;
        }
        int count = Math.min(data.getEntries().size(), TrackerConfig.getMaxSkillsShown());
        return calcBoxHeight(mc, count);
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        HudPosition pos = getPosition();
        return pos.isVisible() && TrackerConfig.showOverlay() && TrackerSyncS2C.ClientTrackerData.serverHasMod();
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.TOP_LEFT_BASED,
                new Insets(0, 0, 0, 0),
                new Insets(0, 0, 0, 0)
        );
    }

    @Override
    public void render(GuiGraphics g, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null && !(mc.screen instanceof DraggableHudConfigScreen)
                && !(mc.screen instanceof ConfigScreen)) return;

        boolean isConfig = mc.screen instanceof DraggableHudConfigScreen;
        HudPosition pos = getPosition();
        if (!isConfig && (!pos.isVisible() || !TrackerConfig.showOverlay())) return;

        TrackerSyncS2C data = TrackerSyncS2C.ClientTrackerData.get();
        List<TrackerSyncS2C.SkillStatsEntry> rows;
        double grandTotal;
        double dps;

        if (data != null && !data.getEntries().isEmpty()) {
            rows = data.getEntries();
            grandTotal = data.getTotalDamage();
            dps = TrackerSyncS2C.ClientTrackerData.getLiveOverallDps();
        } else if (isConfig) {
            TrackerSyncS2C.SkillStatsEntry dummy = new TrackerSyncS2C.SkillStatsEntry(
                "exile_overlay:dummy_skill", "Preview Skill", "dummy_spell", 12345.0,
                10, 2, 0, 1500.0f, 800.0f, 0, 1234.5f, 0.2f, Map.of(), ""
            );
            rows = List.of(dummy);
            grandTotal = 12345.0;
            dps = 1234.5;
        } else {
            return;
        }

        int topN = TrackerConfig.getMaxSkillsShown();
        var font = mc.font;
        int count = Math.min(rows.size(), topN);
        if (count == 0 && !isConfig) return;

        String dmgStr = FormatUtil.fmt(grandTotal);
        String dpsStr = dps > 0 ? FormatUtil.fmt(dps) + "/s" : "0/s";
        String headerLeft = dmgStr;
        String headerRight = dpsStr + " DPS";

        int valW = 0;
        for (int i = 0; i < count; i++) {
            valW = Math.max(valW, font.width(rowValue(rows.get(i), grandTotal)));
        }

        int nameAreaW = 0;
        for (int i = 0; i < count; i++) {
            TrackerSyncS2C.SkillStatsEntry r = rows.get(i);
            int entryW = ICON_S + font.width((i + 1) + ". " + resolveDisplayName(r));
            nameAreaW = Math.max(nameAreaW, entryW);
        }

        int headerW = font.width(headerLeft) + 6 + font.width(headerRight);
        int textAreaW = Math.max(headerW, nameAreaW + NAME_VAL_GAP + valW);

        float scale = pos.getScale();
        float effectiveScale = scale > 0.01f ? scale : 1.0f;

        int maxAvailW = (int) ((ctx.getScreenWidth() - MARGIN * 2) / effectiveScale);
        int boxW = Math.max(MIN_WIDTH, PADDING + textAreaW + PADDING);
        boxW = Math.min(boxW, Math.max(MIN_WIDTH, maxAvailW));

        int headerH = font.lineHeight + 4;
        int maxH = (int) ((ctx.getScreenHeight() - MARGIN) / effectiveScale);
        int maxRows = Math.max(0, (maxH - PADDING * 2 - headerH) / (ROW_H + ROW_GAP));
        count = Math.min(count, maxRows);
        if (isConfig) {
            count = Math.max(1, count);
        }
        if (count <= 0) return;

        int boxH = PADDING + headerH + count * (ROW_H + ROW_GAP) + PADDING;

        int[] resolved = pos.resolve(ctx.getScreenWidth(), ctx.getScreenHeight());

        int x = Math.max(0, Math.min(resolved[0], ctx.getScreenWidth() - (int)(boxW * effectiveScale)));
        int y = Math.max(0, Math.min(resolved[1], ctx.getScreenHeight() - (int)(boxH * effectiveScale)));

        var poseStack = g.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);

        g.fill(0, 0, boxW, boxH, BG_COLOR);

        int ty = PADDING;
        g.drawString(font, headerLeft, PADDING, ty, HEADER_TEXT, true);
        int dpsColor = dps > 0 ? ACCENT_COLOR : 0xFF666666;
        g.drawString(font, headerRight, boxW - PADDING - font.width(headerRight), ty, dpsColor, true);
        ty += headerH;

        int contentLeft = PADDING;
        int contentRight = boxW - PADDING;

        double topDamage = rows.get(0).totalDamage;

        Set<String> activeKeys = new HashSet<>();
        for (int i = 0; i < count; i++) {
            activeKeys.add(rows.get(i).skillId);
        }
        animRatio.keySet().retainAll(activeKeys);

        for (int i = 0; i < count; i++) {
            TrackerSyncS2C.SkillStatsEntry r = rows.get(i);

            double ratio = topDamage > 0 ? r.totalDamage / topDamage : 0;
            float prev = animRatio.getOrDefault(r.skillId, 0f);
            float lerped = animate(prev, (float) ratio);
            animRatio.put(r.skillId, lerped);

            int cursorX = contentLeft;

            if (!r.rawSpellId.isEmpty()) {
                try {
                    ResourceLocation icon = Spell.getIconLoc(r.rawSpellId);
                    if (icon != null && VALID_ICON_CACHE.computeIfAbsent(icon, key -> mc.getResourceManager().getResource(key).isPresent())) {
                        RenderSystem.enableBlend();
                        RenderSystem.setShaderTexture(0, icon);
                        int iconY = ty + (ROW_H - ICON_S) / 2;
                        g.blit(icon, cursorX, iconY, 0, 0, ICON_S, ICON_S, ICON_S, ICON_S);
                        RenderSystem.disableBlend();
                    }
                } catch (Throwable t) {
                    LOGGER.debug("Failed to render spell icon for: {}", r.rawSpellId, t);
                }
            }
            cursorX += ICON_S;

            int barLeft = cursorX;
            int barAreaW = contentRight - barLeft;
            int barW = (int) Math.round(barAreaW * lerped);

            // 属性別セグメントバー描画
            if (!r.elementDamage.isEmpty() && r.totalDamage > 0) {
                // ダメージ降順でソートして安定した描画順を保つ
                List<Map.Entry<String, Double>> sorted = new ArrayList<>(r.elementDamage.entrySet());
                sorted.sort(Map.Entry.<String, Double>comparingByValue().reversed());

                int segX = barLeft;
                int remaining = barW;
                for (int si = 0; si < sorted.size(); si++) {
                    Map.Entry<String, Double> el = sorted.get(si);
                    boolean isLast = (si == sorted.size() - 1);
                    // 最後のセグメントは余りをそのまま使って端数ピクセルの誤差を吸収する
                    int segW = isLast ? remaining : (int) Math.round(barW * (el.getValue() / r.totalDamage));
                    if (segW <= 0) continue;
                    int segColor = (BAR_ALPHA << 24) | (ElementColors.colorFor(el.getKey()) & 0x00FFFFFF);
                    g.fill(segX, ty, segX + segW, ty + ROW_H, segColor);
                    segX += segW;
                    remaining -= segW;
                }
            } else {
                // 属性データなし：従来の単色フォールバック
                int base = ElementColors.colorFor(r.dominantElement);
                int barColor = (BAR_ALPHA << 24) | (base & 0x00FFFFFF);
                g.fill(barLeft, ty, barLeft + barW, ty + ROW_H, barColor);
            }

            int textY = ty + (ROW_H - font.lineHeight) / 2;

            String rankName = (i + 1) + ". " + resolveDisplayName(r);
            int nameSpace = barAreaW - valW - 4;
            String name = FormatUtil.truncate(font, rankName, nameSpace);
            g.drawString(font, name, barLeft + 2, textY, 0xFFE8E8E8, true);

            String val = rowValue(r, grandTotal);
            g.drawString(font, val, contentRight - font.width(val), textY, VALUE_COLOR, true);

            ty += ROW_H + ROW_GAP;
        }

        poseStack.popPose();
    }

    public static int calcBoxWidth(Minecraft mc, int count, List<TrackerSyncS2C.SkillStatsEntry> rows, double grandTotal) {
        var font = mc.font;
        int valW = 0;
        for (int i = 0; i < count; i++) {
            valW = Math.max(valW, font.width(rowValue(rows.get(i), grandTotal)));
        }
        int nameAreaW = 0;
        for (int i = 0; i < count; i++) {
            TrackerSyncS2C.SkillStatsEntry r = rows.get(i);
            int entryW = ICON_S + font.width((i + 1) + ". " + resolveDisplayName(r));
            nameAreaW = Math.max(nameAreaW, entryW);
        }
        int headerW = font.width(FormatUtil.fmt(grandTotal)) + 6 + font.width("0/s DPS");
        int textAreaW = Math.max(headerW, nameAreaW + NAME_VAL_GAP + valW);
        return Math.max(MIN_WIDTH, PADDING + textAreaW + PADDING);
    }

    public static int calcBoxHeight(Minecraft mc, int count) {
        int headerH = mc.font.lineHeight + 4;
        return PADDING + headerH + count * (ROW_H + ROW_GAP) + PADDING;
    }

    static int getPadding() { return PADDING; }
    static int getRowH() { return ROW_H; }
    static int getRowGap() { return ROW_GAP; }
    static int getIconS() { return ICON_S; }

    private static float animate(float current, float target) {
        if (Math.abs(target - current) < 0.002f) return target;
        return current + (target - current) * 0.2f;
    }

    private static String rowValue(TrackerSyncS2C.SkillStatsEntry r, double grandTotal) {
        double pct = grandTotal > 0 ? r.totalDamage / grandTotal * 100 : 0;
        return FormatUtil.fmt(r.totalDamage) + "  " + String.format("%.0f%%", pct);
    }

    public static String resolveDisplayName(TrackerSyncS2C.SkillStatsEntry entry) {
        if (entry == null) return "";
        String displayName = entry.displayName;
        if (displayName == null || displayName.isEmpty()) return "";

        // 1. displayName が言語ファイル/リソースパックに直接存在する場合（最優先）
        //    mmorpg.spell.xxx や exile_overlay.tracker.xxx 等がここで解決される
        if (net.minecraft.client.resources.language.I18n.exists(displayName)) {
            return net.minecraft.client.resources.language.I18n.get(displayName);
        }

        // 2. rawSpellId が空で displayName がキー形式の場合、末尾からIDを抽出
        //    例: entry.rawSpellId="" かつ displayName="mmorpg.spell.sanguine_aura" → rawId="sanguine_aura"
        String rawId = (entry.rawSpellId != null && !entry.rawSpellId.isEmpty()) ? entry.rawSpellId : null;
        if (rawId == null && displayName.startsWith("mmorpg.spell.")) {
            rawId = displayName.substring(13);
        } else if (rawId == null && displayName.startsWith("spell.mineandslash.")) {
            rawId = displayName.substring(19);
        }

        // 3. rawId が判明した場合、M&S DB から loc_name を取得（リソースパック翻訳がない環境向け）
        if (rawId != null) {
            try {
                var spell = com.robertx22.mine_and_slash.database.registry.ExileDB.Spells().get(rawId);
                if (spell != null && spell.loc_name != null && !spell.loc_name.isEmpty()) {
                    return spell.loc_name;
                }
            } catch (Throwable ignored) {
            }
        }

        // 4. ailment: skillId="ailment:burn" の場合、DB から表示名を取得
        if (entry.skillId != null && entry.skillId.startsWith("ailment:")) {
            String ailmentId = entry.skillId.substring(8);
            try {
                var ailment = com.robertx22.mine_and_slash.database.registry.ExileDB.Ailments().get(ailmentId);
                if (ailment != null) {
                    String name = ailment.locNameForLangFile();
                    if (name != null && !name.isEmpty()) return name;
                }
            } catch (Throwable ignored) {
            }
        }

        // 5. Component.translatable による翻訳試行（上記で解決できない翻訳キーへの保険）
        try {
            String translated = net.minecraft.network.chat.Component.translatable(displayName).getString();
            if (!translated.equals(displayName)) return translated;
        } catch (Throwable ignored) {
        }

        // 6. 最終フォールバック：キー文字列の末尾IDを整形して表示
        //    例: "mmorpg.spell.sanguine_aura" → "Sanguine Aura"
        if (displayName.contains(".")) {
            String lastPart = displayName.substring(displayName.lastIndexOf('.') + 1);
            return capitalizeWords(lastPart.replace('_', ' '));
        }

        return displayName;
    }

    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return "";
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
