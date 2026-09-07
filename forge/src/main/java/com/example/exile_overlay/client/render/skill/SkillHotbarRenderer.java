package com.example.exile_overlay.client.render.skill;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.HudFontHelper;
import com.example.exile_overlay.client.render.util.CooldownRenderHelper;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.util.SpellKeyHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class SkillHotbarRenderer implements IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillHotbarRenderer.class);

    public static final int SLOT_COUNT = 8;
    public static final int SLOT_SIZE = 32;
    public static final int SLOT_SPACING = 1;
    public static final int ICON_SIZE = 26;
    public static final int ICON_OFFSET = 3;

    private static final ResourceLocation BASE_FRAME_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/skill_slot_base.png");
    private static final ResourceLocation SLOT_BG_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/skill_slot_background.png");
    private static final ResourceLocation KEYBIND_FRAME_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/skill_slot_keybind.png");
    private static final ResourceLocation KEYBIND_MOD_FRAME_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/skill_slot_keybind_mod.png");
    private static final ResourceLocation SUMMON_BADGE_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/skill_slot_summon_badge.png");
    private static final ResourceLocation CHARGE_BADGE_TEXTURE = ResourceLocation.tryParse(
            "exile_overlay:textures/gui/skill_slot_charge_badge.png");

    private static final class SlotRenderData {
        int slot;
        int slotX;
        int slotY;
        int iconX;
        int iconY;
        ResourceLocation icon;
        float renderPercent;
        float smoothedRegen;
        float smoothedCd;
    }

    private final SlotRenderData[] slotDataCache = new SlotRenderData[SLOT_COUNT];

    public SkillHotbarRenderer() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slotDataCache[i] = new SlotRenderData();
        }
    }

    private static final int COOLDOWN_OVERLAY_COLOR = 0xAA000000;
    private static final int SUMMON_TEXT_COLOR = 0xFFFF5555;
    private static final int CHARGE_COLOR_FULL = 0xFF00FF00;
    private static final int CHARGE_COLOR_PARTIAL = 0xFFFFFF00;
    private static final int CHARGE_COLOR_EMPTY = 0xFFFF4444;

    private static final String[] KEY_MODIFIERS = {"LEFT SHIFT", "RIGHT SHIFT", "SHIFT",
            "左SHIFT", "右SHIFT", "シフト",
            "LEFT CONTROL", "RIGHT CONTROL", "CONTROL", "LEFT CTRL", "RIGHT CTRL", "CTRL",
            "左CTRL", "右CTRL", "左CONTROL", "右CONTROL", "コントロール",
            "LEFT ALT", "RIGHT ALT", "ALT",
            "左ALT", "右ALT", "オルト"};
    private static final String[] KEY_MOD_REPLACEMENTS = {"s", "s", "s", "s", "s", "s",
            "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c",
            "a", "a", "a", "a", "a", "a"};
    private static final String[][] KEY_MAPPINGS = {
            {"MOUSE BUTTON", "M"}, {"マウスボタン", "M"}, {"マウス", "M"},
            {"BUTTON", "M"}, {"ボタン", "M"},
            {"CAPS LOCK", "Caps"}, {"キャプスロック", "Caps"},
            {"BACKSPACE", "Bksp"}, {"バックスペース", "Bksp"},
            {"ESCAPE", "Esc"}, {"エスケープ", "Esc"},
            {"PAGE UP", "PgUp"}, {"ページアップ", "PgUp"},
            {"PAGE DOWN", "PgDn"}, {"ページダウン", "PgDn"},
            {"PRINT SCREEN", "PSc"},
            {"SCROLL LOCK", "SLk"},
            {"NUM LOCK", "NLk"},
            {"ARROW UP", "↑"}, {"ARROW DOWN", "↓"},
            {"ARROW LEFT", "←"}, {"ARROW RIGHT", "→"},
            {"INSERT", "Ins"}, {"インサート", "Ins"},
            {"DELETE", "Del"}, {"デリート", "Del"},
            {"ENTER", "Ent"},
            {"PAUSE", "Pau"},
            {"SPACE", "Sp"}, {"スペース", "Sp"},
            {"NUMPAD", "N"},
            {"DIGIT", ""},
            {"WORLD", "W"},
    };

    @Override
    public String getId() {
        return "skill_hotbar";
    }

    @Override
    public String getConfigKey() {
        return "skill_hotbar";
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        if (!MethodHandlesUtil.isAvailable()) {
            return false;
        }
        if (countActualSkills(ctx.getPlayer()) == 0) {
            return false;
        }
        return IRenderCommand.super.isVisible(ctx);
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = ctx.getMinecraft();
        Player player = ctx.getPlayer();
        if (player == null || mc.options.hideGui) {
            return;
        }

        if (!MethodHandlesUtil.isAvailable()) {
            return;
        }

        HudPosition position = getPosition();
        boolean horizontal = position.isHorizontal();

        int totalWidth = getConfigWidth();
        int totalHeight = getConfigHeight();

        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();
        int[] pos = position.resolve(screenWidth, screenHeight);
        int x = pos[0];
        int y = pos[1];
        float scale = position.getScale();

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        // 中心基準：設定画面と同じ計算方法を使用
        // 設定画面：left = x - (width * scale) / 2
        // レンダリング：translate(x, y) 後、スケール後座標系で描画
        // スケール後座標系では1単位 = scaleピクセルなので、
        // スケール後座標系で (width * scale / 2) / scale = width / 2 移動すれば良い
        int offsetX = -totalWidth / 2;
        int offsetY = -totalHeight / 2;

        float gcdPercent = MethodHandlesUtil.getGlobalCooldownPercent(player);
        int gcdNeededTicks = MethodHandlesUtil.getGlobalCooldownNeededTicks(player);
        int gcdLeft = MethodHandlesUtil.getGlobalCooldownTicks(player);
        float smoothedGcd = CooldownSmoothedValue.getSmoothedGcd(gcdPercent, gcdNeededTicks);

        // Pass 1: スロット背景・アイコンの描画とデータ収集
        int visibleCount = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ResourceLocation icon = MethodHandlesUtil.getSpellIcon(player, slot);

            if (icon == null && !EquipmentDisplayConfig.getInstance().isShowEmptySkillSlots()) {
                continue;
            }

            int slotX, slotY;
            if (horizontal) {
                slotX = offsetX + visibleCount * (SLOT_SIZE + SLOT_SPACING);
                slotY = offsetY;
            } else {
                slotX = offsetX;
                slotY = offsetY + visibleCount * (SLOT_SIZE + SLOT_SPACING);
            }

            int iconX = slotX + ICON_OFFSET;
            int iconY = slotY + ICON_OFFSET;

            SlotRenderData data = slotDataCache[visibleCount];
            data.slot = slot;
            data.slotX = slotX;
            data.slotY = slotY;
            data.iconX = iconX;
            data.iconY = iconY;
            data.icon = icon;
            data.renderPercent = 0.0f;
            data.smoothedRegen = 0.0f;
            data.smoothedCd = 0.0f;

            RenderSystem.enableBlend();
            graphics.blit(SLOT_BG_TEXTURE, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

            if (icon != null) {
                RenderSystem.enableBlend();
                graphics.blit(icon, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

                if (MethodHandlesUtil.getSpellUsesCharges(player, slot)) {
                    int charges = MethodHandlesUtil.getSpellCharges(player, slot);
                    int maxCharges = MethodHandlesUtil.getSpellMaxCharges(player, slot);

                    if (charges < maxCharges) {
                        float regenPercent = MethodHandlesUtil.getSpellChargeRegenPercent(player, slot);
                        data.smoothedRegen = CooldownSmoothedValue.getSmoothedChargeRegen(slot, regenPercent);
                    } else {
                        CooldownSmoothedValue.getSmoothedChargeRegen(slot, 0.0f);
                    }

                    if (smoothedGcd > 0) {
                        data.renderPercent = smoothedGcd;
                    }
                } else {
                    float cdPercent = MethodHandlesUtil.getSpellCooldownPercent(player, slot);
                    int cdLeft = MethodHandlesUtil.getSpellCooldownTicks(player, slot);
                    int cdNeed = MethodHandlesUtil.getSpellNeededTicks(player, slot);
                    data.smoothedCd = CooldownSmoothedValue.getSmoothedCooldown(slot, cdPercent, cdNeed);

                    int longestLeft = 0;
                    if (cdLeft > 1 && cdNeed > 0 && cdLeft > longestLeft) {
                        longestLeft = cdLeft;
                        data.renderPercent = data.smoothedCd;
                    }

                    if (gcdLeft > 1 && gcdNeededTicks > 0 && gcdLeft > longestLeft) {
                        longestLeft = gcdLeft;
                        data.renderPercent = smoothedGcd;
                    }
                }
            }

            visibleCount++;
        }

        // アイコンまでの描画を確実にGPUへフラッシュ
        graphics.flush();

        // Pass 2: クールダウンオーバーレイ & チャージ回復バー描画（アイコンの前、枠の後ろ）
        for (int i = 0; i < visibleCount; i++) {
            SlotRenderData data = slotDataCache[i];
            if (data.icon != null) {
                if (data.renderPercent > 0) {
                    drawCooldownOverlay(graphics, data.iconX, data.iconY, data.renderPercent);
                }
                if (data.smoothedRegen > 0) {
                    drawChargeRegenBar(graphics, data.iconX, data.iconY, data.smoothedRegen);
                }
            }
        }

        // クールダウン描画を確実にフラッシュ
        graphics.flush();

        // Pass 3: スロット枠の描画（クールダウンの前）
        for (int i = 0; i < visibleCount; i++) {
            SlotRenderData data = slotDataCache[i];
            RenderSystem.enableBlend();
            graphics.blit(BASE_FRAME_TEXTURE, data.slotX, data.slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        }

        // 枠描画を確実にフラッシュ
        graphics.flush();

        // Pass 4: キーバインド枠、キーバインド文字、クールダウン数値、バッジ（枠の前）
        boolean isSimpleKeybind = EquipmentDisplayConfig.getInstance().isSimpleSkillKeybindDisplay();
        boolean isSimpleBadge = EquipmentDisplayConfig.getInstance().isSimpleSkillChargeSummonDisplay();
        for (int i = 0; i < visibleCount; i++) {
            SlotRenderData data = slotDataCache[i];
            int slot = data.slot;
            int slotX = data.slotX;
            int slotY = data.slotY;
            int iconX = data.iconX;
            int iconY = data.iconY;

            String rawKeyText = SpellKeyHelper.getSpellKeyText(slot);
            String displayKey = abbreviateKeyText(rawKeyText);

            if (!displayKey.isEmpty() && !isSimpleKeybind) {
                RenderSystem.enableBlend();
                ResourceLocation keybindFrame = displayKey.length() >= 2
                        ? KEYBIND_MOD_FRAME_TEXTURE : KEYBIND_FRAME_TEXTURE;
                graphics.blit(keybindFrame, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
            }

            if (!displayKey.isEmpty()) {
                int fullTextWidth = HudFontHelper.getTextWidth(mc.font, displayKey);
                float textScale = isSimpleKeybind ? 1.25f : 0.8f;
                float keyX, keyY;
                if (isSimpleKeybind) {
                    keyX = slotX + 3.0f;
                    keyY = slotY + 20.0f;
                } else {
                    float frameCenterX = displayKey.length() >= 2 ? 20.5f : 24.5f;
                    keyX = slotX + frameCenterX - fullTextWidth * textScale / 2.0f;
                    keyY = slotY + 25.0f - mc.font.lineHeight * textScale / 2.0f;
                }

                graphics.pose().pushPose();
                graphics.pose().translate(keyX, keyY, 0);
                graphics.pose().scale(textScale, textScale, 1.0f);

                int plusIndex = displayKey.lastIndexOf('+');
                if (plusIndex >= 0) {
                    String modKeyPart = displayKey.substring(0, plusIndex);
                    String mainKeyPart = displayKey.substring(plusIndex + 1);
                    int modKeyWidth = HudFontHelper.getTextWidth(mc.font, modKeyPart);
                    int plusWidth = HudFontHelper.getTextWidth(mc.font, "+");
                    
                    if (isSimpleKeybind) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                if (dx != 0 || dy != 0) {
                                    HudFontHelper.drawString(graphics, mc.font, displayKey, dx, dy, 0xFF000000, false);
                                }
                            }
                        }
                    }
                    HudFontHelper.drawString(graphics, mc.font, modKeyPart, 0, 0, 0xFF55FF55, false);
                    HudFontHelper.drawString(graphics, mc.font, "+", modKeyWidth, 0, 0xFFFFFF55, false);
                    HudFontHelper.drawString(graphics, mc.font, mainKeyPart, modKeyWidth + plusWidth, 0, 0xFFFFFFFF, false);
                } else {
                    if (isSimpleKeybind) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                if (dx != 0 || dy != 0) {
                                    HudFontHelper.drawString(graphics, mc.font, displayKey, dx, dy, 0xFF000000, false);
                                }
                            }
                        }
                    }
                    HudFontHelper.drawString(graphics, mc.font, displayKey, 0, 0, 0xFFFFFFFF, false);
                }

                graphics.pose().popPose();
            }

            if (data.icon != null) {
                if (data.smoothedCd > 0 && EquipmentDisplayConfig.getInstance().isShowSkillCooldownNumber()) {
                    int seconds = MethodHandlesUtil.getSpellCooldownSeconds(player, slot);
                    if (seconds > 0) {
                        String text = String.valueOf(seconds);
                        float cdTextScale = 1.5f;
                        int textWidth = HudFontHelper.getTextWidth(mc.font, text);
                        float textX = (iconX + ICON_SIZE / 2.0f - textWidth * cdTextScale / 2.0f + 1) / cdTextScale;
                        float textY = (iconY + ICON_SIZE / 2.0f - mc.font.lineHeight * cdTextScale / 2.0f) / cdTextScale;
                        graphics.pose().pushPose();
                        try {
                            graphics.pose().scale(cdTextScale, cdTextScale, 1.0f);
                            HudFontHelper.drawString(graphics, mc.font, text, (int) textX + 1, (int) textY + 1, 0xFF000000, false);
                            HudFontHelper.drawString(graphics, mc.font, text, (int) textX, (int) textY, 0xFFFFFF00, false);
                        } finally {
                            graphics.pose().popPose();
                        }
                    }
                }

                if (EquipmentDisplayConfig.getInstance().isShowSkillSummonCount()) {
                    int summonCount = MethodHandlesUtil.getSummonCount(player, slot);
                    if (summonCount > 0) {
                        drawSummonBadge(graphics, mc, slotX, slotY, summonCount, isSimpleBadge);
                    }
                }

                if (MethodHandlesUtil.getSpellUsesCharges(player, slot)) {
                    int charges = MethodHandlesUtil.getSpellCharges(player, slot);
                    int maxCharges = MethodHandlesUtil.getSpellMaxCharges(player, slot);
                    drawChargeBadge(graphics, mc, slotX, slotY, charges, maxCharges, isSimpleBadge);
                }
            }
        }

        graphics.pose().popPose();
    }

    private void drawSummonBadge(GuiGraphics graphics, Minecraft mc, int slotX, int slotY, int count, boolean isSimpleBadge) {
        if (!isSimpleBadge) {
            RenderSystem.enableBlend();
            graphics.blit(SUMMON_BADGE_TEXTURE, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        }

        String text = String.valueOf(count);
        int textWidth = HudFontHelper.getTextWidth(mc.font, text);
        int textHeight = mc.font.lineHeight;

        float s = isSimpleBadge ? 1.0f : 0.8f;
        float textX = slotX + 2.0f + (8.0f - textWidth * s) / 2.0f + 0.5f + 1;
        float textY = slotY + 2.5f + (8.0f - textHeight * s) / 2.0f + 1;
        if (isSimpleBadge) {
            // Adjust position for larger text scale without badge
            textX = slotX + 7.0f - (textWidth * s) / 2.0f; // moved right 1px
            textY = slotY + 4.0f; 
        }

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(s, s, 1.0f);
        if (isSimpleBadge) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        HudFontHelper.drawString(graphics, mc.font, text, dx, dy, 0xFF000000, false);
                    }
                }
            }
        }
        HudFontHelper.drawString(graphics, mc.font, text, 0, 0, SUMMON_TEXT_COLOR, false);
        graphics.pose().popPose();
    }

    private void drawChargeBadge(GuiGraphics graphics, Minecraft mc, int slotX, int slotY, int charges, int maxCharges, boolean isSimpleBadge) {
        if (!isSimpleBadge) {
            RenderSystem.enableBlend();
            graphics.blit(CHARGE_BADGE_TEXTURE, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        }

        boolean showMax = isSimpleBadge && EquipmentDisplayConfig.getInstance().isSimpleSkillChargeMaxDisplay();
        String text = showMax ? (charges + "/" + maxCharges) : String.valueOf(charges);
        int textWidth = HudFontHelper.getTextWidth(mc.font, text);
        int textHeight = mc.font.lineHeight;

        float s = isSimpleBadge ? (showMax ? 0.75f : 1.0f) : 0.8f;
        float textX = slotX + 23.0f + (8.0f - textWidth * s) / 2.0f - 0.5f - 1;
        float textY = slotY + 2.5f + (8.0f - textHeight * s) / 2.0f + 1;
        if (isSimpleBadge) {
            textX = showMax ? (slotX + 29.0f - textWidth * s) : (slotX + 26.0f - (textWidth * s) / 2.0f);
            textY = slotY + 4.0f;
        }

        int color;
        if (charges <= 0) {
            color = CHARGE_COLOR_EMPTY;
        } else if (charges < maxCharges) {
            color = CHARGE_COLOR_PARTIAL;
        } else {
            color = CHARGE_COLOR_FULL;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(s, s, 1.0f);
        if (isSimpleBadge) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        HudFontHelper.drawString(graphics, mc.font, text, dx, dy, 0xFF000000, false);
                    }
                }
            }
        }
        HudFontHelper.drawString(graphics, mc.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private String abbreviateKeyText(String raw) {
        if (raw.isEmpty()) return "";

        String text = raw;
        String upper = text.toUpperCase(Locale.ROOT);

        for (int i = 0; i < KEY_MODIFIERS.length; i++) {
            int idx = upper.indexOf(KEY_MODIFIERS[i].toUpperCase(Locale.ROOT));
            if (idx >= 0) {
                text = text.substring(0, idx) + KEY_MOD_REPLACEMENTS[i] + text.substring(idx + KEY_MODIFIERS[i].length());
                upper = text.toUpperCase(Locale.ROOT);
            }
        }

        for (String[] mapping : KEY_MAPPINGS) {
            int idx = upper.indexOf(mapping[0].toUpperCase(Locale.ROOT));
            if (idx >= 0) {
                text = text.substring(0, idx) + mapping[1] + text.substring(idx + mapping[0].length());
                upper = text.toUpperCase(Locale.ROOT);
            }
        }

        text = text.replace(" + ", "+");
        text = text.replace(" ", "");

        return text;
    }

    private void drawCooldownOverlay(GuiGraphics graphics, int x, int y, float percent) {
        CooldownRenderHelper.drawCooldown(graphics, x, y, ICON_SIZE, ICON_SIZE, percent,
                COOLDOWN_OVERLAY_COLOR, EquipmentDisplayConfig.getInstance().getCooldownDisplayType());
    }

    private void drawChargeRegenBar(GuiGraphics graphics, int iconX, int iconY, float percent) {
        float barHeight = 2.0f;
        float y = iconY + ICON_SIZE - barHeight;

        // 左右に2pxずつ余裕を持たせる（先ほどより左右1pxずつ拡大）
        float barWidth = ICON_SIZE - 4.0f;
        float x = iconX + 2.0f;

        // percentは1.0(回復開始)から0.0(完了)へ減少していく値なので、進捗バーの長さは (1.0 - percent)
        float fillWidth = barWidth * (1.0f - percent);

        // 背景 (半透明の黒)
        fillFloat(graphics, x, y, x + barWidth, y + barHeight, 0x88000000);
        // プログレス (黄色系、右下バッジの色に近い色)
        fillFloat(graphics, x, y, x + fillWidth, y + barHeight, 0xFFFFFF00);
    }

    private void fillFloat(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
        org.joml.Matrix4f matrix4f = graphics.pose().last().pose();
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        com.mojang.blaze3d.vertex.BufferBuilder bufferbuilder = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        
        bufferbuilder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(matrix4f, minX, maxY, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix4f, maxX, maxY, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix4f, maxX, minY, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix4f, minX, minY, 0.0F).color(r, g, b, a).endVertex();
        
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(bufferbuilder.end());
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Override
    public int getWidth() {
        HudPosition position = getPosition();
        if (position.isHorizontal()) {
            return (SLOT_SIZE * SLOT_COUNT) + (SLOT_SPACING * (SLOT_COUNT - 1));
        } else {
            return SLOT_SIZE;
        }
    }

    @Override
    public int getHeight() {
        HudPosition position = getPosition();
        if (position.isHorizontal()) {
            return SLOT_SIZE;
        } else {
            return (SLOT_SIZE * SLOT_COUNT) + (SLOT_SPACING * (SLOT_COUNT - 1));
        }
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    /**
     * 実際に表示されているスキルの数をカウント
     * (設定で空スロット表示がONの場合はSLOT_COUNTを返す)
     */
    private int countVisibleSkills(Player player) {
        if (player == null || !MethodHandlesUtil.isAvailable()) {
            return 0;
        }
        if (EquipmentDisplayConfig.getInstance().isShowEmptySkillSlots()) {
            return SLOT_COUNT;
        }

        return countActualSkills(player);
    }

    /**
     * スロットにセットされている実際のスキル数をカウント
     */
    private int countActualSkills(Player player) {
        if (player == null || !MethodHandlesUtil.isAvailable()) {
            return 0;
        }

        int count = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ResourceLocation icon = MethodHandlesUtil.getSpellIcon(player, slot);
            if (icon != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 設定画面用の幅を取得
     * 表示されているスキル数に応じた動的サイズを返す
     */
    @Override
    public int getConfigWidth() {
        Minecraft mc = Minecraft.getInstance();
        int visibleCount = countVisibleSkills(mc.player);
        int count = Math.max(visibleCount, 1);

        HudPosition position = getPosition();
        if (position.isHorizontal()) {
            return (SLOT_SIZE * count) + (SLOT_SPACING * (count - 1));
        } else {
            return SLOT_SIZE;
        }
    }

    /**
     * 設定画面用の高さを取得
     * 表示されているスキル数に応じた動的サイズを返す
     */
    @Override
    public int getConfigHeight() {
        Minecraft mc = Minecraft.getInstance();
        int visibleCount = countVisibleSkills(mc.player);
        int count = Math.max(visibleCount, 1);

        HudPosition position = getPosition();
        if (position.isHorizontal()) {
            return SLOT_SIZE;
        } else {
            return (SLOT_SIZE * count) + (SLOT_SPACING * (count - 1));
        }
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.CENTER_BASED,
                new Insets(0, 0, 0, 0),
                new Insets(5, 5, 4, 5)
        );
    }
}
