package com.example.exile_overlay.client.render.effect;

import com.example.exile_overlay.util.MineAndSlashHelper;
import com.example.exile_overlay.util.MineAndSlashHelper.ExileEffectInfo;
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
 */
public class EffectRenderHelper {

    // Animation constants
    private static final float ANIMATION_SPEED = 0.2f;
    private static final int FLASH_DURATION = 10;

    // Global state maps to track visual position across frames
    private static final Map<String, VisualState> displayStates = new HashMap<>();

    public static class VisualState {
        public float currentX;
        public float currentY;
        public boolean isNew;
        public int flashTimer;
        public int maxDuration;

        public VisualState(float startX) {
            this.currentX = startX;
            this.isNew = true;
            this.flashTimer = FLASH_DURATION;
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

    // Wrapper for Vanilla Effects
    public static class VanillaEffectWrapper implements DisplayableEffect {
        private final MobEffectInstance instance;
        private final TextureAtlasSprite sprite;

        public VanillaEffectWrapper(MobEffectInstance instance, TextureAtlasSprite sprite) {
            this.instance = instance;
            this.sprite = sprite;
        }

        @Override
        public String getId() {
            return "vanilla:" + MobEffect.getId(instance.getEffect());
        }

        @Override
        public ResourceLocation getTexture() {
            return null;
        }

        @Override
        public TextureAtlasSprite getSprite() {
            return sprite;
        }

        @Override
        public boolean isBeneficial() {
            return instance.getEffect().getCategory() == MobEffectCategory.BENEFICIAL;
        }

        @Override
        public boolean isInfinite() {
            return instance.isInfiniteDuration();
        }

        @Override
        public int getDuration() {
            return instance.getDuration();
        }

        @Override
        public int getStacks() {
            return instance.getAmplifier() + 1;
        }

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

    // Wrapper for Mine and Slash Effects
    public static class MnSEffectWrapper implements DisplayableEffect {
        private final ExileEffectInfo info;

        public MnSEffectWrapper(ExileEffectInfo info) {
            this.info = info;
        }

        @Override
        public String getId() {
            return "mns:" + info.id;
        }

        @Override
        public ResourceLocation getTexture() {
            return info.texture;
        }

        @Override
        public TextureAtlasSprite getSprite() {
            return null;
        }

        @Override
        public boolean isBeneficial() {
            return info.isBeneficial;
        }

        @Override
        public boolean isInfinite() {
            return info.isInfinite;
        }

        @Override
        public int getDuration() {
            return info.isInfinite ? Integer.MAX_VALUE : info.duration;
        }

        @Override
        public int getStacks() {
            return info.stacks;
        }

        @Override
        public String getDurationText() {
            return info.durationText;
        }

        @Override
        public void renderIcon(GuiGraphics graphics, int x, int y, int size) {
            if (info.texture != null) {
                RenderSystem.setShaderTexture(0, info.texture);
                graphics.blit(info.texture, x, y, 0, 0, size, size, size, size);
            }
        }
    }

    public static List<DisplayableEffect> getUnifiedEffects(Player player, boolean beneficial) {
        List<DisplayableEffect> combined = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        // 1. Gather Vanilla
        for (MobEffectInstance effect : player.getActiveEffects()) {
            boolean isBen = effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL;
            if (isBen == beneficial) {
                TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effect.getEffect());
                combined.add(new VanillaEffectWrapper(effect, sprite));
            }
        }

        // 2. Gather Mine and Slash
        List<ExileEffectInfo> mnsEffects = beneficial
                ? MineAndSlashHelper.getExileBuffs(player)
                : MineAndSlashHelper.getExileDebuffs(player);

        for (ExileEffectInfo info : mnsEffects) {
            combined.add(new MnSEffectWrapper(info));
        }

        // 3. Sort: Infinite -> Longest -> Shortest
        combined.sort((a, b) -> {
            boolean aInf = a.isInfinite();
            boolean bInf = b.isInfinite();

            if (aInf && !bInf) return -1;
            if (!aInf && bInf) return 1;
            if (aInf && bInf) return 0;

            return Integer.compare(b.getDuration(), a.getDuration());
        });

        return combined;
    }

    private static void updateVisualStates(List<DisplayableEffect> currentEffects) {
        Set<String> currentIds = new HashSet<>();
        for (DisplayableEffect effect : currentEffects) {
            currentIds.add(effect.getId());
        }

        displayStates.keySet().removeIf(id -> !currentIds.contains(id));
    }

    public static VisualState getVisualState(String id, float targetX, int duration) {
        VisualState state = displayStates.get(id);
        if (state == null) {
            state = new VisualState(targetX);
            state.maxDuration = duration;
            displayStates.put(id, state);
        } else if (duration > state.maxDuration) {
            state.maxDuration = duration;
        }
        return state;
    }

    public static void tick() {
        for (VisualState state : displayStates.values()) {
            if (state.flashTimer > 0) {
                state.flashTimer--;
            }
        }
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
        if (seconds >= 3600) {
            return (seconds / 3600) + "h";
        } else if (seconds >= 60) {
            return (seconds / 60) + "m";
        } else {
            return seconds + "s";
        }
    }
}
