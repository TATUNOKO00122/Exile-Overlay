package com.example.exile_overlay.client.render.effect;

import com.example.exile_overlay.client.config.BuffOverlayFilterConfig;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.api.data.ExileEffectInfo;
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
        int getStacks();
        String getDurationText();
        void renderIcon(GuiGraphics graphics, int x, int y, int size);
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
            boolean isBeneficial = effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL;
            if (isBeneficial && !filter.isShowVanillaBuffs()) continue;
            if (!isBeneficial && !filter.isShowVanillaDebuffs()) continue;
            addVanillaEffect(mc, effect);
        }

        if (filter.isShowMnsBuffs() || filter.isShowMnsDebuffs()) {
            for (ExileEffectInfo info : MethodHandlesUtil.getPlayerExileEffects(player)) {
                if (info.isBeneficial && !filter.isShowMnsBuffs()) continue;
                if (!info.isBeneficial && !filter.isShowMnsDebuffs()) continue;
                addMnsEffect(info);
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
