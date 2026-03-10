package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Day Counter Renderer - Typewriter-style animation
 * Displays "— DAY X —" when a new day starts
 * 
 * 【機能】
 * - 新しいゲーム内の日にちが始まるとタイプライターアニメーションで日数を表示
 * - フレームレート非依存（ゲームティックベース）
 * - 音声フィードバック付き
 */
public class DayCounterRenderer implements IRenderCommand, IHudRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DayCounterRenderer.class);
    private static final String COMMAND_ID = "day_counter";

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

    // State
    private long lastDay = -1;
    private int animationTick = 0;
    private boolean isAnimating = false;
    private long currentDisplayDay = 0;
    private long animationStartTime = 0;

    // Configuration key
    private static final String CONFIG_KEY = "day_counter";

    public DayCounterRenderer() {
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
        HudPosition pos = HudPositionManager.getInstance().getPosition(CONFIG_KEY);
        return pos.isVisible();
    }

    @Override
    public void execute(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx);
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        // Get current day (Minecraft day = dayTime / 24000)
        long dayTime = mc.level.getDayTime();
        long currentDay = dayTime / 24000L + 1; // +1 to make Day 1 the first day
        long timeOfDay = dayTime % 24000L;

        // Detect new day (time 0-1000 is early morning)
        if (currentDay != lastDay && timeOfDay < 1000) {
            if (lastDay != -1) { // Don't trigger on first load
                startAnimation(currentDay, mc);
            }
            lastDay = currentDay;
        }

        // Update and render animation
        if (isAnimating) {
            updateAnimation(graphics, ctx, mc);
        }
    }

    private void startAnimation(long day, Minecraft mc) {
        isAnimating = true;
        animationTick = 0;
        currentDisplayDay = day;
        if (mc.level != null) {
            animationStartTime = mc.level.getGameTime();
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
            int rawWidth = mc.font.width(text);
            int scaledWidth = (int) (rawWidth * scale);

            // Center position
            int x = (ctx.getScreenWidth() - scaledWidth) / 2;
            int y = ctx.getScreenHeight() / 2 - 40; // Slightly above center

            // Draw with scale
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            graphics.drawString(mc.font, text, 0, 0, 0xFFFFFF, true);
            graphics.pose().popPose();

            // Play sound on certain frames
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

        // Play sound on key frames
        if (tick == ANIM_START || tick == ANIM_DASH2 || tick == ANIM_SPACE ||
            tick == ANIM_D || tick == ANIM_DA || tick == ANIM_DAY || tick == ANIM_NUMBER) {
            mc.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.MASTER, 0.4f, 2.0f);
        }
    }

    /**
     * Force trigger animation (for testing)
     */
    public void forceShow(long day) {
        Minecraft mc = Minecraft.getInstance();
        startAnimation(day, mc);
    }

    // IHudRenderer implementation

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
