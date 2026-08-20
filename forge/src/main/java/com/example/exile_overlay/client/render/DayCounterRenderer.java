package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Day Counter Renderer - Typewriter-style animation
 * 新しいゲーム内の日になると「— DAY X —」をタイプライターアニメーションで表示。
 * サーバー遅延・パケットジッターによる反復発火防止、音声フィードバック付き。
 */
public class DayCounterRenderer implements IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DayCounterRenderer.class);
    private static final String COMMAND_ID = "day_counter";
    private static final HudPositionManager POSITION_MANAGER = HudPositionManager.getInstance();
    private static final DayCounterConfig DAY_CONFIG = DayCounterConfig.getInstance();

    // Animation timing (in game ticks, 20 ticks = 1 second)
    private static final int ANIM_START = 10;      // — (0.5秒)
    private static final int ANIM_DASH2 = 20;      // —— (1秒)
    private static final int ANIM_SPACE = 30;      // — — (1.5秒)
    private static final int ANIM_D = 40;          // — D — (2秒)
    private static final int ANIM_DA = 50;         // — DA — (2.5秒)
    private static final int ANIM_DAY = 60;        // — DAY — (3秒)
    private static final int ANIM_NUMBER = 70;     // — DAY X — (3.5秒)
    private static final int ANIM_WAIT_DURATION = 80; // 待機時間 (4秒)
    private static final int ANIM_END = ANIM_NUMBER + ANIM_WAIT_DURATION; // 150 (7.5秒)

    // クールダウン（アニメーション開始後、意図的な過去日リセット等を安全に受け入れるまでの保護時間：10秒）
    private static final long COOLDOWN_TICKS = 200L;

    // Singleton / Instance reference
    private static volatile DayCounterRenderer instance;

    // State
    private long lastDay = -1;
    private long lastAnimatedDay = -1;
    private long lastAnimationStartTime = -1;
    private int animationTick = 0;
    private boolean isAnimating = false;
    private long currentDisplayDay = 0;
    private long animationStartTime = 0;
    private int lastSoundTick = -1;
    private Object lastLevelIdentity = null;

    // Configuration key
    private static final String CONFIG_KEY = "day_counter";
    private static final SoundEvent TYPEWRITER_SOUND = SoundEvent.createVariableRangeEvent(new ResourceLocation("minecraft:ui.stonecutter.select_recipe"));

    public DayCounterRenderer() {
        instance = this;
    }

    public static DayCounterRenderer getInstance() {
        return instance;
    }

    /**
     * セッション終了時やワールド切断時に状態をリセット
     */
    public static void reset() {
        if (instance != null) {
            instance.resetState();
        }
    }

    /**
     * 内部状態を初期化
     */
    public void resetState() {
        lastDay = -1;
        lastAnimatedDay = -1;
        lastAnimationStartTime = -1;
        animationTick = 0;
        isAnimating = false;
        currentDisplayDay = 0;
        animationStartTime = 0;
        lastSoundTick = -1;
        lastLevelIdentity = null;
        LOGGER.debug("Reset DayCounterRenderer state");
    }

    @Override
    public String getId() {
        return COMMAND_ID;
    }

    @Override
    public int getPriority() {
        return 200; // High priority to render on top
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.OVERLAY;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        // Always visible when animating
        if (isAnimating) {
            return true;
        }
        // Check config visibility
        HudPosition pos = POSITION_MANAGER.getPosition(CONFIG_KEY);
        return pos.isVisible();
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        // ワールド/ディメンション切り替え検知
        Object currentLevelIdentity = mc.level;
        if (lastLevelIdentity != currentLevelIdentity) {
            lastLevelIdentity = currentLevelIdentity;
            long dayTime = mc.level.getDayTime();
            long initialDay = Math.max(1L, Math.floorDiv(dayTime, 24000L) + 1L);
            lastDay = initialDay;
            lastAnimatedDay = initialDay;
            isAnimating = false;
            animationTick = 0;
            lastSoundTick = -1;
            return;
        }

        long dayTime = mc.level.getDayTime();
        long currentDay = Math.max(1L, Math.floorDiv(dayTime, 24000L) + 1L);
        long currentGameTime = mc.level.getGameTime();

        if (lastDay == -1) {
            // 初回フレーム：現在日を記録（ワールド参加直後の誤発火防止）
            lastDay = currentDay;
            lastAnimatedDay = currentDay;
        } else if (currentDay > lastDay && currentDay > lastAnimatedDay) {
            // 日数が新しく進んだ場合のみアニメーションを開始
            startAnimation(currentDay, mc);
            lastAnimatedDay = currentDay;
            lastDay = currentDay;
        } else if (currentDay < lastDay) {
            // 時刻が巻き戻った場合（サーバーラグまたは /time set 等）
            lastDay = currentDay;
            // クールダウン経過後の巻き戻りのみ lastAnimatedDay を同期（意図的なコマンドリセット等に対応）
            // クールダウン中（一時的なパケットジッター）は lastAnimatedDay を維持して再発火を抑止
            if (lastAnimationStartTime < 0 || (currentGameTime - lastAnimationStartTime) > COOLDOWN_TICKS) {
                lastAnimatedDay = currentDay;
            }
        }

        // Update and render animation
        if (isAnimating) {
            updateAnimation(graphics, ctx, mc);
        }
    }

    private void startAnimation(long day, Minecraft mc) {
        isAnimating = true;
        animationTick = 0;
        lastSoundTick = -1;
        currentDisplayDay = day;
        if (mc.level != null) {
            animationStartTime = mc.level.getGameTime();
            lastAnimationStartTime = animationStartTime;
        }
        LOGGER.debug("Day counter animation started for day {}", day);
    }

    private void updateAnimation(GuiGraphics graphics, RenderContext ctx, Minecraft mc) {
        // Update animation based on game ticks (frame rate independent)
        if (mc.level != null) {
            long currentGameTime = mc.level.getGameTime();
            animationTick = (int) (currentGameTime - animationStartTime);
        }

        String text = getAnimationText(animationTick);
        if (text != null) {
            float scale = getScale();
            int rawWidth = HudFontHelper.getTextWidth(mc.font, text);
            int scaledWidth = (int) (rawWidth * scale);

            // Center position
            int x = (ctx.getScreenWidth() - scaledWidth) / 2;
            int y = ctx.getScreenHeight() / 2 - 40; // Slightly above center

            // Draw with scale
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            HudFontHelper.drawString(graphics, mc.font, text, 0, 0, 0xFFFFFF, true);
            graphics.pose().popPose();

            // Play sound on key frames
            playTypeSound(mc, animationTick);
        }

        // End animation
        if (animationTick >= ANIM_END) {
            isAnimating = false;
            animationTick = 0;
        }
    }

    private String getAnimationText(int tick) {
        if (tick >= ANIM_NUMBER && tick <= ANIM_END) {
            return "— DAY " + currentDisplayDay + " —";
        } else if (tick >= ANIM_DAY && tick < ANIM_NUMBER) {
            return "— DAY —";
        } else if (tick >= ANIM_DA && tick < ANIM_DAY) {
            return "— DA —";
        } else if (tick >= ANIM_D && tick < ANIM_DA) {
            return "— D —";
        } else if (tick >= ANIM_SPACE && tick < ANIM_D) {
            return "— —";
        } else if (tick >= ANIM_DASH2 && tick < ANIM_SPACE) {
            return "——";
        } else if (tick >= ANIM_START && tick < ANIM_DASH2) {
            return "—";
        }
        return null;
    }

    private void playTypeSound(Minecraft mc, int tick) {
        if (mc.player == null) {
            return;
        }

        // 同一tick内での重複再生を防止（フレームレート依存の音割れ/爆音化を防止）
        if (tick == lastSoundTick) {
            return;
        }

        float volume = DAY_CONFIG.getSoundVolumeFloat();
        if (volume <= 0.0f) {
            return;
        }

        // Play sound on key frames
        if (tick == ANIM_START || tick == ANIM_DASH2 || tick == ANIM_SPACE ||
            tick == ANIM_D || tick == ANIM_DA || tick == ANIM_DAY || tick == ANIM_NUMBER) {
            lastSoundTick = tick;
            mc.player.playNotifySound(TYPEWRITER_SOUND, SoundSource.MASTER, volume, 1.90f);
        }
    }

    /**
     * Force trigger animation (for testing)
     */
    public void forceShow(long day) {
        Minecraft mc = Minecraft.getInstance();
        startAnimation(day, mc);
    }

    @Override
    public String getConfigKey() {
        return CONFIG_KEY;
    }

    @Override
    public int getWidth() {
        return 120; // Approximate width for "— DAY 999 —"
    }

    @Override
    public int getHeight() {
        return 20;
    }

    @Override
    public boolean isDraggable() {
        return false; // Fixed position in center
    }
}
