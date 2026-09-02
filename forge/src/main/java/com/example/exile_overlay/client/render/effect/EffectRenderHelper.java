package com.example.exile_overlay.client.render.effect;

import com.example.exile_overlay.client.config.BuffOverlayFilterConfig;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.api.data.ExileEffectInfo;
import com.example.exile_overlay.api.data.MercenaryDisplayInfo;
import com.example.exile_overlay.api.data.MinionDisplayInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * バフ/デバフの統合管理とレンダリングヘルパー
 * VanillaとMine and Slashの効果を統合して表示します。
 *
 * - オブジェクトプールによるWrapper再利用
 * - キャッシュ済みリストによるアロケーション削減
 */
public class EffectRenderHelper {

    private static final BuffOverlayFilterConfig FILTER_CONFIG = BuffOverlayFilterConfig.getInstance();
    private static final ResourceLocation DEFAULT_MINION_ICON = new ResourceLocation("exile_overlay",
            "textures/gui/skill_slot_summon_badge.png");
    private static final float ANIMATION_SPEED = 0.2f;
    private static final float FADE_IN_SPEED = 0.06f;
    private static final float SLIDE_DISTANCE = 30.0f;
    private static final float SLIDE_SPEED = 0.08f;

    private static final Map<String, VisualState> displayStates = new HashMap<>();

    private static final List<DisplayableEffect> cachedBuffs = new ArrayList<>(32);
    private static final List<DisplayableEffect> cachedDebuffs = new ArrayList<>(32);
    private static final List<DisplayableEffect> filteredEffectsCache = new ArrayList<>(32);
    private static final Map<String, VanillaEffectWrapper> vanillaWrapperCache = new HashMap<>();
    private static final Map<String, MnSEffectWrapper> mnsWrapperCache = new HashMap<>();
    private static final Map<String, MinionEffectWrapper> minionWrapperCache = new HashMap<>();
    private static final Set<String> currentIdsCache = new HashSet<>(32);
    private static final Map<String, Long> effectOrderMap = new HashMap<>();
    private static long nextOrderSequence = 0L;

    private static long getOrAssignOrder(String id) {
        return effectOrderMap.computeIfAbsent(id, k -> ++nextOrderSequence);
    }

    public static class VisualState {
        public float currentX;
        public float currentY;
        public float alpha;
        public float offsetX;
        public int maxDuration;

        public VisualState(float startX) {
            this.currentX = startX;
            this.alpha = 0.0f;
            this.offsetX = SLIDE_DISTANCE;
            this.maxDuration = -1;
        }
    }

    public interface DisplayableEffect {
        String getId();
        ResourceLocation getTexture();
        TextureAtlasSprite getSprite();
        boolean isBeneficial();
        boolean isInfinite();
        int getDuration();
        default int getMaxDuration() { return getDuration(); }
        int getStacks();
        default boolean showStackCount() { return getStacks() > 1; }
        default String getCustomStackText() { return null; }
        String getDurationText();
        void renderIcon(GuiGraphics graphics, int x, int y, int size);
        default boolean isMercenary() { return false; }
        default MercenaryDisplayInfo getMercenaryInfo() { return null; }
        default boolean isMinion() { return false; }
    }

    public static class VanillaEffectWrapper implements DisplayableEffect {
        private MobEffectInstance instance;
        private TextureAtlasSprite sprite;

        public VanillaEffectWrapper(MobEffectInstance instance, TextureAtlasSprite sprite) {
            this.instance = instance;
            this.sprite = sprite;
        }

        public void updateInstance(MobEffectInstance instance) {
            this.instance = instance;
        }

        public void updateSprite(TextureAtlasSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public String getId() {
            return "vanilla:" + MobEffect.getId(instance.getEffect());
        }

        @Override
        public ResourceLocation getTexture() { return null; }

        @Override
        public TextureAtlasSprite getSprite() { return sprite; }

        @Override
        public boolean isBeneficial() {
            return instance.getEffect().getCategory() == MobEffectCategory.BENEFICIAL;
        }

        @Override
        public boolean isInfinite() { return instance.isInfiniteDuration(); }

        @Override
        public int getDuration() { return instance.getDuration(); }

        @Override
        public int getStacks() { return instance.getAmplifier() + 1; }

        @Override
        public String getDurationText() {
            if (isInfinite()) return "**";
            return formatDuration(getDuration() / 20);
        }

        @Override
        public void renderIcon(GuiGraphics graphics, int x, int y, int size) {
            RenderSystem.setShaderTexture(0, sprite.atlasLocation());
            graphics.blit(x, y, 0, size, size, sprite);
        }
    }

    public static class MnSEffectWrapper implements DisplayableEffect {
        private ExileEffectInfo info;

        public MnSEffectWrapper(ExileEffectInfo info) {
            this.info = info;
        }

        public void updateInfo(ExileEffectInfo info) {
            this.info = info;
        }

        @Override
        public String getId() { return "mns:" + info.id; }

        @Override
        public ResourceLocation getTexture() { return info.texture; }

        @Override
        public TextureAtlasSprite getSprite() { return null; }

        @Override
        public boolean isBeneficial() { return info.isBeneficial; }

        @Override
        public boolean isInfinite() { return info.isInfinite; }

        @Override
        public int getDuration() { return info.isInfinite ? Integer.MAX_VALUE : info.duration; }

        @Override
        public int getStacks() { return info.stacks; }

        @Override
        public String getDurationText() { return info.durationText; }

        @Override
        public void renderIcon(GuiGraphics graphics, int x, int y, int size) {
            if (info.texture != null) {
                RenderSystem.setShaderTexture(0, info.texture);
                graphics.blit(info.texture, x, y, size, size, 0, 0, 16, 16, 16, 16);
            }
        }
    }

    public static class BlockCooldownEffectWrapper implements DisplayableEffect {
        private static final ResourceLocation TEXTURE = new ResourceLocation("mmorpg", "textures/gui/block_disabled.png");
        private int currentTicks;
        private int neededTicks;

        public void update(int currentTicks, int neededTicks) {
            this.currentTicks = currentTicks;
            this.neededTicks = neededTicks;
        }

        @Override
        public String getId() { return "mns:block_cooldown"; }

        @Override
        public ResourceLocation getTexture() { return TEXTURE; }

        @Override
        public TextureAtlasSprite getSprite() { return null; }

        @Override
        public boolean isBeneficial() { return false; }

        @Override
        public boolean isInfinite() { return false; }

        @Override
        public int getDuration() { return currentTicks; }

        @Override
        public int getMaxDuration() { return neededTicks > 0 ? neededTicks : currentTicks; }

        @Override
        public int getStacks() { return 1; }

        @Override
        public String getDurationText() {
            int seconds = (currentTicks + 19) / 20;
            return formatDuration(seconds);
        }

        @Override
        public void renderIcon(GuiGraphics graphics, int x, int y, int size) {
            RenderSystem.setShaderTexture(0, TEXTURE);
            graphics.blit(TEXTURE, x, y, size, size, 0, 0, 16, 16, 16, 16);
        }
    }

    public static class MinionEffectWrapper implements DisplayableEffect {
        private MinionDisplayInfo minion;

        public MinionEffectWrapper(MinionDisplayInfo minion) {
            this.minion = minion;
        }

        public void updateInfo(MinionDisplayInfo minion) {
            this.minion = minion;
        }

        @Override
        public String getId() { return "mns:minion:" + minion.spellId(); }

        @Override
        public ResourceLocation getTexture() {
            return minion.icon() != null ? minion.icon() : DEFAULT_MINION_ICON;
        }

        @Override
        public TextureAtlasSprite getSprite() { return null; }

        @Override
        public boolean isBeneficial() { return true; }

        @Override
        public boolean isInfinite() { return minion.isInfinite(); }

        @Override
        public int getDuration() { return minion.isInfinite() ? Integer.MAX_VALUE : minion.durationTicks(); }

        @Override
        public int getMaxDuration() {
            return minion.maxDurationTicks() > 0 ? minion.maxDurationTicks() : minion.durationTicks();
        }

        @Override
        public int getStacks() { return minion.count(); }

        @Override
        public boolean showStackCount() { return minion.count() > 0; }

        @Override
        public String getCustomStackText() { return String.valueOf(minion.count()); }

        @Override
        public String getDurationText() { return minion.durationText(); }

        @Override
        public void renderIcon(GuiGraphics graphics, int x, int y, int size) {
            ResourceLocation icon = getTexture();
            if (icon != null) {
                RenderSystem.setShaderTexture(0, icon);
                graphics.blit(icon, x, y, size, size, 0, 0, 16, 16, 16, 16);
            }
        }

        @Override
        public boolean isMinion() { return true; }
    }

    public static class MercenaryEffectWrapper implements DisplayableEffect {
        private MercenaryDisplayInfo merc;

        public MercenaryEffectWrapper(MercenaryDisplayInfo merc) {
            this.merc = merc;
        }

        public void updateInfo(MercenaryDisplayInfo merc) {
            this.merc = merc;
        }

        @Override
        public boolean isMercenary() {
            return true;
        }

        @Override
        public MercenaryDisplayInfo getMercenaryInfo() {
            return merc;
        }

        @Override
        public String getId() {
            return "mns:mercenary:" + (merc != null ? merc.classId() : "none");
        }

        @Override
        public ResourceLocation getTexture() {
            return merc != null && merc.icon() != null ? merc.icon() : DEFAULT_MINION_ICON;
        }

        @Override
        public TextureAtlasSprite getSprite() {
            return null;
        }

        @Override
        public boolean isBeneficial() {
            return true;
        }

        @Override
        public boolean isInfinite() {
            return true;
        }

        @Override
        public int getDuration() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxDuration() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getStacks() {
            return merc != null ? merc.level() : 1;
        }

        @Override
        public boolean showStackCount() {
            return false;
        }

        @Override
        public String getCustomStackText() {
            return merc != null ? String.valueOf(merc.level()) : null;
        }

        @Override
        public String getDurationText() {
            return null;
        }

        @Override
        public void renderIcon(GuiGraphics graphics, int x, int y, int size) {
            ResourceLocation icon = getTexture();
            if (icon != null) {
                int srcSize = (icon.getPath().contains("summon_zombie") || icon.getPath().contains("skill_slot_summon_badge")) ? 16 : 36;
                RenderSystem.setShaderTexture(0, icon);
                graphics.blit(icon, x, y, size, size, 0, 0, srcSize, srcSize, srcSize, srcSize);
            }
        }
    }

    private static final BlockCooldownEffectWrapper blockCooldownWrapper = new BlockCooldownEffectWrapper();
    private static final MercenaryEffectWrapper mercenaryWrapper = new MercenaryEffectWrapper(null);

    private static final java.util.Comparator<DisplayableEffect> DEFAULT_ORDER_COMPARATOR = (a, b) -> {
        return Long.compare(getOrAssignOrder(a.getId()), getOrAssignOrder(b.getId()));
    };

    private static final java.util.Comparator<DisplayableEffect> EFFECT_COMPARATOR = (a, b) -> {
        boolean aInf = a.isInfinite();
        boolean bInf = b.isInfinite();
        if (aInf && !bInf) return -1;
        if (!aInf && bInf) return 1;
        if (aInf && bInf) return Long.compare(getOrAssignOrder(a.getId()), getOrAssignOrder(b.getId()));
        int durCmp = Integer.compare(b.getDuration(), a.getDuration());
        if (durCmp != 0) return durCmp;
        return Long.compare(getOrAssignOrder(a.getId()), getOrAssignOrder(b.getId()));
    };

    /**
     * 指定オーバーレイのフィルタ設定に基づいてエフェクトを取得する統一メソッド
     *
     * @param player 対象プレイヤー
     * @param overlayId "buff_overlay" or "skill_buff_overlay"
     */
    public static List<DisplayableEffect> getFilteredEffects(Player player, String overlayId) {
        Minecraft mc = Minecraft.getInstance();
        BuffOverlayFilterConfig.OverlayFilter filter =
                FILTER_CONFIG.getFilter(overlayId);

        filteredEffectsCache.clear();

        for (MobEffectInstance effect : player.getActiveEffects()) {
            addVanillaEffect(mc, effect);
        }

        if (MethodHandlesUtil.isAvailable()) {
            for (ExileEffectInfo info : MethodHandlesUtil.getPlayerExileEffects(player)) {
                addMnsEffect(info);
            }

            if (MethodHandlesUtil.isBlockOnCooldown(player)) {
                int blockCdTicks = MethodHandlesUtil.getBlockCooldownTicks(player);
                int blockCdNeeded = MethodHandlesUtil.getBlockCooldownNeededTicks(player);
                if (blockCdTicks <= 0) blockCdTicks = 1;
                blockCooldownWrapper.update(blockCdTicks, blockCdNeeded);
                filteredEffectsCache.add(blockCooldownWrapper);
            }
        }

        if (filter.isShowMinions() && MethodHandlesUtil.isAvailable()) {
            for (MinionDisplayInfo minion : MethodHandlesUtil.getActiveMinions(player)) {
                addMinionEffect(minion);
            }
        }

        // すべての有効エフェクトに付与順シリアルIDを登録
        for (DisplayableEffect effect : filteredEffectsCache) {
            getOrAssignOrder(effect.getId());
        }

        if (filter.isSortByDuration()) {
            filteredEffectsCache.sort(EFFECT_COMPARATOR);
        } else {
            filteredEffectsCache.sort(DEFAULT_ORDER_COMPARATOR);
        }

        // 傭兵はトラックの先頭に常駐（M&S本家の仕様に準拠）
        if (filter.isShowMercenary() && MethodHandlesUtil.isAvailable()) {
            MercenaryDisplayInfo merc = MethodHandlesUtil.getActiveMercenary(player);
            if (merc != null && merc.isAlive()) {
                mercenaryWrapper.updateInfo(merc);
                getOrAssignOrder(mercenaryWrapper.getId());
                filteredEffectsCache.add(0, mercenaryWrapper);
            }
        }

        return filteredEffectsCache;
    }

    private static void addVanillaEffect(Minecraft mc, MobEffectInstance effect) {
        String cacheKey = "vanilla:" + MobEffect.getId(effect.getEffect());
        VanillaEffectWrapper wrapper = vanillaWrapperCache.get(cacheKey);
        if (wrapper == null) {
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effect.getEffect());
            wrapper = new VanillaEffectWrapper(effect, sprite);
            vanillaWrapperCache.put(cacheKey, wrapper);
        } else {
            wrapper.updateInstance(effect);
        }
        filteredEffectsCache.add(wrapper);
    }

    private static void addMnsEffect(ExileEffectInfo info) {
        String cacheKey = "mns:" + info.id;
        MnSEffectWrapper wrapper = mnsWrapperCache.get(cacheKey);
        if (wrapper == null) {
            wrapper = new MnSEffectWrapper(info);
            mnsWrapperCache.put(cacheKey, wrapper);
        } else {
            wrapper.updateInfo(info);
        }
        filteredEffectsCache.add(wrapper);
    }

    private static void addMinionEffect(MinionDisplayInfo minion) {
        String cacheKey = "mns:minion:" + minion.spellId();
        MinionEffectWrapper wrapper = minionWrapperCache.get(cacheKey);
        if (wrapper == null) {
            wrapper = new MinionEffectWrapper(minion);
            minionWrapperCache.put(cacheKey, wrapper);
        } else {
            wrapper.updateInfo(minion);
        }
        filteredEffectsCache.add(wrapper);
    }

    /**
     * @deprecated getFilteredEffects を使用
     */
    @Deprecated
    public static List<DisplayableEffect> getUnifiedEffects(Player player, boolean beneficial) {
        Minecraft mc = Minecraft.getInstance();
        List<DisplayableEffect> cachedResult = beneficial ? cachedBuffs : cachedDebuffs;
        cachedResult.clear();

        for (MobEffectInstance effect : player.getActiveEffects()) {
            boolean isBen = effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL;
            if (isBen == beneficial) {
                String cacheKey = "vanilla:" + MobEffect.getId(effect.getEffect());
                VanillaEffectWrapper wrapper = vanillaWrapperCache.get(cacheKey);
                if (wrapper == null) {
                    TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effect.getEffect());
                    wrapper = new VanillaEffectWrapper(effect, sprite);
                    vanillaWrapperCache.put(cacheKey, wrapper);
                } else {
                    wrapper.updateInstance(effect);
                }
                cachedResult.add(wrapper);
            }
        }

        List<ExileEffectInfo> mnsEffects = MethodHandlesUtil.getPlayerExileEffects(player);

        for (ExileEffectInfo info : mnsEffects) {
            String cacheKey = "mns:" + info.id;
            MnSEffectWrapper wrapper = mnsWrapperCache.get(cacheKey);
            if (wrapper == null) {
                wrapper = new MnSEffectWrapper(info);
                mnsWrapperCache.put(cacheKey, wrapper);
            } else {
                wrapper.updateInfo(info);
            }
            cachedResult.add(wrapper);
        }

        cachedResult.sort(EFFECT_COMPARATOR);
        return cachedResult;
    }

    public static VisualState getVisualState(String namespace, String id, float targetX, int duration) {
        String namespacedId = namespace + ":" + id;
        VisualState state = displayStates.get(namespacedId);
        if (state == null) {
            state = new VisualState(targetX);
            state.maxDuration = duration;
            displayStates.put(namespacedId, state);
        } else if (duration > state.maxDuration) {
            state.maxDuration = duration;
        }
        return state;
    }

    public static void updateFadeIn(VisualState state) {
        if (state.alpha < 1.0f) {
            state.alpha = Math.min(1.0f, state.alpha + FADE_IN_SPEED);
        }
        if (state.offsetX > 0.5f) {
            state.offsetX += (0.0f - state.offsetX) * SLIDE_SPEED;
        } else {
            state.offsetX = 0.0f;
        }
    }

    public static void updateVisualStates(String namespace, List<DisplayableEffect> currentEffects) {
        String prefix = namespace + ":";
        currentIdsCache.clear();
        for (DisplayableEffect effect : currentEffects) {
            currentIdsCache.add(prefix + effect.getId());
        }
        displayStates.keySet().removeIf(id -> id.startsWith(prefix) && !currentIdsCache.contains(id));
        effectOrderMap.keySet().removeIf(id -> !currentIdsCache.contains(prefix + id) && !currentIdsCache.contains("buff_overlay:" + id) && !currentIdsCache.contains("skill_buff_overlay:" + id));
    }

    public static void clearCache() {
        vanillaWrapperCache.clear();
        mnsWrapperCache.clear();
        minionWrapperCache.clear();
        displayStates.clear();
        effectOrderMap.clear();
        nextOrderSequence = 0L;
        cachedBuffs.clear();
        cachedDebuffs.clear();
        filteredEffectsCache.clear();
    }

    public static float updatePosition(VisualState state, float targetX, float partialTick) {
        float diff = targetX - state.currentX;
        if (Math.abs(diff) < 0.5f) {
            state.currentX = targetX;
        } else {
            state.currentX += diff * ANIMATION_SPEED;
        }
        return state.currentX;
    }

    private static String formatDuration(int seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h";
        if (seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }
}
