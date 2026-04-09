package com.example.exile_overlay.client.render;

import com.example.exile_overlay.client.config.HudFontConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * HUD要素用フォント管理クラス
 * 
 * 【責任】
 * - Google Sansフォントの読み込み・管理
 * - Minecraftフォントとの自動比率計算
 * - HUDテキスト描画の統一API提供
 */
public class HudFontManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/HudFontManager");
    private static HudFontManager instance;
    
    private static final ResourceLocation GOOGLE_SANS_FONT = 
        ResourceLocation.tryParse("exile_overlay:font/google_sans_bold.ttf");
    
    private java.awt.Font customFont;
    private boolean fontLoaded = false;
    private boolean fontLoadAttempted = false;
    
    private final Map<String, StringCacheData> stringCache = new HashMap<>();
    
    private float baseScale = 1.0f;
    private int fontSize = 9;
    
    private static class StringCacheData {
        final int width;
        final int height;
        final int textureId;
        
        StringCacheData(int width, int height, int textureId) {
            this.width = width;
            this.height = height;
            this.textureId = textureId;
        }
    }

    private HudFontManager() {
    }

    public static HudFontManager getInstance() {
        if (instance == null) {
            instance = new HudFontManager();
        }
        return instance;
    }

    public void initialize() {
        HudFontConfig config = HudFontConfig.getInstance();
        if (!config.isUseCustomFont()) {
            return;
        }
    }

    private void lazyLoadFont() {
        if (fontLoaded || fontLoadAttempted) {
            return;
        }
        
        fontLoadAttempted = true;
        
        HudFontConfig config = HudFontConfig.getInstance();
        if (!config.isUseCustomFont()) {
            return;
        }
        
        loadGoogleSansFont();
    }

    private void loadGoogleSansFont() {
        try {
            Minecraft mc = Minecraft.getInstance();
            
            try (InputStream is = mc.getResourceManager().getResource(GOOGLE_SANS_FONT).get().open()) {
                customFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is)
                    .deriveFont(java.awt.Font.BOLD, fontSize);
            }
            
            calculateScaleRatio();
            
            fontLoaded = true;
            LOGGER.info("Loaded Google Sans font for HUD (size: {}, scale: {})", fontSize, baseScale);
            
        } catch (FontFormatException e) {
            LOGGER.error("Invalid font format: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to load Google Sans font: {}", e.getMessage());
        }
    }

    private void calculateScaleRatio() {
        Minecraft mc = Minecraft.getInstance();
        Font minecraftFont = mc.font;
        
        int mcLineHeight = minecraftFont.lineHeight;
        
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImage.createGraphics();
        g2d.setFont(customFont);
        
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = customFont.getStringBounds("A", frc);
        
        int customHeight = (int) Math.ceil(bounds.getHeight());
        g2d.dispose();
        
        baseScale = mcLineHeight / (float) customHeight;
        
        LOGGER.debug("Scale ratio calculated: Minecraft lineHeight={}, Custom height={}, scale={}", 
            mcLineHeight, customHeight, baseScale);
    }

    public void drawString(PoseStack poseStack, String text, float x, float y, int color, boolean shadow) {
        lazyLoadFont();
        
        HudFontConfig config = HudFontConfig.getInstance();
        
        if (!config.isUseCustomFont() || !fontLoaded) {
            drawWithMinecraftFont(poseStack, text, x, y, color, shadow);
            return;
        }
        
        drawWithCustomFont(poseStack, text, x, y, color, shadow);
    }

    public void drawStringWithScale(PoseStack poseStack, String text, float x, float y, 
                                    int color, float additionalScale, boolean shadow) {
        lazyLoadFont();
        
        HudFontConfig config = HudFontConfig.getInstance();
        
        if (!config.isUseCustomFont() || !fontLoaded) {
            poseStack.pushPose();
            poseStack.scale(additionalScale, additionalScale, 1.0f);
            drawWithMinecraftFont(poseStack, text, x / additionalScale, y / additionalScale, color, shadow);
            poseStack.popPose();
            return;
        }
        
        drawWithCustomFontAndScale(poseStack, text, x, y, color, additionalScale, shadow);
    }

    private void drawWithMinecraftFont(PoseStack poseStack, String text, float x, float y, 
                                       int color, boolean shadow) {
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        
        if (shadow) {
            mc.font.drawInBatch(text, x + 1.0f, y + 1.0f, (color & 0xFCFCFC) >> 2, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            mc.font.drawInBatch(text, x, y, color, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        } else {
            mc.font.drawInBatch(text, x, y, color, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        }
        
        bufferSource.endBatch();
    }

    private void drawWithCustomFont(PoseStack poseStack, String text, float x, float y, 
                                    int color, boolean shadow) {
        StringCacheData cacheData = getStringCacheData(text);
        if (cacheData == null) {
            drawWithMinecraftFont(poseStack, text, x, y, color, shadow);
            return;
        }
        
        float width = cacheData.width;
        float height = cacheData.height;
        int textureId = cacheData.textureId;
        
        if (shadow) {
            renderTexture(poseStack, x + 1.0f * baseScale, y + 1.0f * baseScale, 
                width, height, textureId, (color & 0xFCFCFC) >> 2, baseScale);
        }
        renderTexture(poseStack, x, y, width, height, textureId, color, baseScale);
    }

    private void drawWithCustomFontAndScale(PoseStack poseStack, String text, float x, float y, 
                                            int color, float additionalScale, boolean shadow) {
        StringCacheData cacheData = getStringCacheData(text);
        if (cacheData == null) {
            LOGGER.warn("getStringCacheData returned null for text: {}", text);
            drawWithMinecraftFont(poseStack, text, x, y, color, shadow);
            return;
        }
        
        float scale = baseScale * additionalScale;
        float width = cacheData.width;
        float height = cacheData.height;
        int textureId = cacheData.textureId;
        
        LOGGER.info("Drawing text '{}' at ({}, {}) scale={} width={} height={} texId={}", 
            text, x, y, scale, width, height, textureId);
        
        if (shadow) {
            renderTexture(poseStack, x + 1.0f * scale, y + 1.0f * scale, 
                width, height, textureId, (color & 0xFCFCFC) >> 2, scale);
        }
        renderTexture(poseStack, x, y, width, height, textureId, color, scale);
    }

    private void renderTexture(PoseStack poseStack, float x, float y, 
                               float width, float height, int textureId, int color, float scale) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        buffer.vertex(matrix, x, y, 0.0f)
              .uv(0.0f, 0.0f)
              .color(r, g, b, a)
              .endVertex();
        
        buffer.vertex(matrix, x + width * scale, y, 0.0f)
              .uv(1.0f, 0.0f)
              .color(r, g, b, a)
              .endVertex();
        
        buffer.vertex(matrix, x + width * scale, y + height * scale, 0.0f)
              .uv(1.0f, 1.0f)
              .color(r, g, b, a)
              .endVertex();
        
        buffer.vertex(matrix, x, y + height * scale, 0.0f)
              .uv(0.0f, 1.0f)
              .color(r, g, b, a)
              .endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
    }

    private StringCacheData getStringCacheData(String text) {
        StringCacheData cached = stringCache.get(text);
        if (cached != null) {
            return cached;
        }

        BufferedImage image = renderStringToImage(text);
        if (image == null) {
            return null;
        }

        int textureId = createTextureFromImage(image);
        if (textureId == -1) {
            return null;
        }

        if (stringCache.size() >= 100) {
            String oldestKey = stringCache.keySet().iterator().next();
            StringCacheData oldestData = stringCache.remove(oldestKey);
            if (oldestData != null && oldestData.textureId != -1) {
                GL11.glDeleteTextures(oldestData.textureId);
            }
        }

        StringCacheData data = new StringCacheData(image.getWidth(), image.getHeight(), textureId);
        stringCache.put(text, data);
        return data;
    }

    private BufferedImage renderStringToImage(String text) {
        Graphics2D g2d = null;
        
        try {
            BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            g2d = tempImage.createGraphics();
            g2d.setFont(customFont);
            
            FontRenderContext frc = g2d.getFontRenderContext();
            Rectangle2D bounds = customFont.getStringBounds(text, frc);
            
            int width = (int) Math.ceil(bounds.getWidth());
            int height = (int) Math.ceil(bounds.getHeight());
            
            g2d.dispose();
            
            if (width <= 0 || height <= 0) {
                return null;
            }
            
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            g2d = image.createGraphics();
            
            g2d.setFont(customFont);
            g2d.setColor(Color.WHITE);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                                RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                                RenderingHints.VALUE_RENDER_QUALITY);
            
            int baseline = (int) Math.ceil(-bounds.getY());
            g2d.drawString(text, 0, baseline);
            
            return image;
            
        } catch (Exception e) {
            LOGGER.error("Failed to render string to image: {}", e.getMessage());
            return null;
        } finally {
            if (g2d != null) {
                g2d.dispose();
            }
        }
    }

    private int createTextureFromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        int bufferSize = width * height * 4;
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(bufferSize);
        directBuffer.order(ByteOrder.nativeOrder());

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            directBuffer.put((byte) ((pixel >> 16) & 0xFF));
            directBuffer.put((byte) ((pixel >> 8) & 0xFF));
            directBuffer.put((byte) (pixel & 0xFF));
            directBuffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        directBuffer.flip();

        int textureId = GL11.glGenTextures();
        if (textureId == 0) {
            LOGGER.error("Failed to generate OpenGL texture");
            return -1;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                         width, height, 0,
                         GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                         directBuffer);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return textureId;
    }

    public int getStringWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }

    public int getLineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    public boolean isFontLoaded() {
        return fontLoaded;
    }

    public void reloadFont() {
        clearCache();
        fontLoaded = false;
        fontLoadAttempted = false;
        customFont = null;
    }

    public void clearCache() {
        for (StringCacheData data : stringCache.values()) {
            if (data.textureId != -1) {
                GL11.glDeleteTextures(data.textureId);
            }
        }
        stringCache.clear();
    }

    public void cleanup() {
        clearCache();
        fontLoaded = false;
        fontLoadAttempted = false;
        customFont = null;
    }
}