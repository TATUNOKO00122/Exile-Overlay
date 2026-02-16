package com.example.exile_overlay.client.damage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/**
 * カスタムTrueTypeフォントを使用してテキストを描画するクラス
 */
public class CustomDamageFontRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/CustomFont");
    private static CustomDamageFontRenderer instance;
    
    private java.awt.Font customFont;
    private boolean fontLoaded = false;
    private String fontPath;
    private int fontSize = 64;
    
    // グリフキャッシュ
    private final Map<Character, GlyphData> glyphCache = new HashMap<>();
    private final Map<String, StringCacheData> stringCache = new HashMap<>();
    
    private static class GlyphData {
        final int width;
        final int height;
        final int advance;
        final BufferedImage image;
        
        GlyphData(int width, int height, int advance, BufferedImage image) {
            this.width = width;
            this.height = height;
            this.advance = advance;
            this.image = image;
        }
    }
    
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

    private CustomDamageFontRenderer() {
    }

    public static CustomDamageFontRenderer getInstance() {
        if (instance == null) {
            instance = new CustomDamageFontRenderer();
        }
        return instance;
    }

    /**
     * カスタムフォントを読み込む（ファイルパスから）
     * @param path フォントファイルのパス（TTF）
     * @param size フォントサイズ
     * @return 読み込み成功したか
     */
    public boolean loadFont(String path, int size) {
        this.fontPath = path;
        this.fontSize = size;
        
        try {
            Path fontFilePath = Paths.get(path);
            if (!Files.exists(fontFilePath)) {
                LOGGER.error("Font file not found: {}", path);
                return false;
            }
            
            // TTFファイルを読み込み
            try (InputStream is = Files.newInputStream(fontFilePath)) {
                customFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is)
                    .deriveFont(java.awt.Font.PLAIN, size);
            }
            
            // キャッシュをクリア
            glyphCache.clear();
            stringCache.clear();
            
            fontLoaded = true;
            LOGGER.info("Loaded custom font: {} (size: {})", path, size);
            return true;
            
        } catch (FontFormatException e) {
            LOGGER.error("Invalid font format: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.error("Failed to load font: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * MODリソースからフォントを読み込む
     * @param resourcePath リソースパス（例: "assets/exile_overlay/font/TitanOne-Regular.ttf"）
     * @param size フォントサイズ
     * @return 読み込み成功したか
     */
    public boolean loadFontFromResource(String resourcePath, int size) {
        this.fontPath = resourcePath;
        this.fontSize = size;
        
        try {
            Minecraft mc = Minecraft.getInstance();
            ResourceLocation location = new ResourceLocation("exile_overlay", resourcePath);
            
            // リソースからフォントを読み込み
            try (InputStream is = mc.getResourceManager().getResource(location).get().open()) {
                customFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is)
                    .deriveFont(java.awt.Font.PLAIN, size);
            }
            
            // キャッシュをクリア
            glyphCache.clear();
            stringCache.clear();
            
            fontLoaded = true;
            LOGGER.info("Loaded custom font from resource: {} (size: {})", resourcePath, size);
            return true;
            
        } catch (FontFormatException e) {
            LOGGER.error("Invalid font format: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to load font from resource: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * カスタムフォントを使用してテキストを描画
     */
    public void renderText(PoseStack poseStack, String text, float x, float y, 
                          int color, MultiBufferSource bufferSource, int packedLight) {
        if (!fontLoaded || customFont == null) {
            // フォントが読み込まれていない場合はデフォルトフォントを使用
            DamageFontRenderer.renderText(poseStack, text, x, y, color, bufferSource, packedLight);
            return;
        }
        
        try {
            renderWithCustomFont(poseStack, text, x, y, color);
        } catch (Exception e) {
            LOGGER.error("Failed to render with custom font, falling back to default", e);
            DamageFontRenderer.renderText(poseStack, text, x, y, color, bufferSource, packedLight);
        }
    }
    
    /**
     * カスタムフォントでの描画処理
     */
    private void renderWithCustomFont(PoseStack poseStack, String text, float x, float y, int color) {
        // キャッシュからデータを取得（必要に応じて生成）
        StringCacheData cacheData = getStringCacheData(text);
        if (cacheData == null) {
            return;
        }
        
        float width = cacheData.width;
        float height = cacheData.height;
        int textureId = cacheData.textureId;
        float scale = 0.1f; // 3D空間でのスケール調整
        
        // 色の分解
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
        
        // 左上
        buffer.vertex(matrix, x, y, 0.0f)
              .uv(0.0f, 0.0f)
              .color(r, g, b, a)
              .endVertex();
        
        // 右上
        buffer.vertex(matrix, x + width * scale, y, 0.0f)
              .uv(1.0f, 0.0f)
              .color(r, g, b, a)
              .endVertex();
        
        // 右下
        buffer.vertex(matrix, x + width * scale, y + height * scale, 0.0f)
              .uv(1.0f, 1.0f)
              .color(r, g, b, a)
              .endVertex();
        
        // 左下
        buffer.vertex(matrix, x, y + height * scale, 0.0f)
              .uv(0.0f, 1.0f)
              .color(r, g, b, a)
              .endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
    }
    
    /**
     * 文字列のキャッシュデータを取得（必要に応じて生成）
     */
    private StringCacheData getStringCacheData(String text) {
        // キャッシュをチェック
        StringCacheData cached = stringCache.get(text);
        if (cached != null) {
            return cached;
        }

        // 新しくレンダリング
        BufferedImage image = renderStringToImage(text);
        if (image == null) {
            return null;
        }

        // OpenGLテクスチャを作成
        int textureId = createTextureFromImage(image);
        if (textureId == -1) {
            return null;
        }

        // キャッシュサイズ制限（古いエントリを削除）
        if (stringCache.size() >= 100) {
            // 古いテクスチャを削除
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
    
    /**
     * 文字列をBufferedImageにレンダリング
     */
    private BufferedImage renderStringToImage(String text) {
        Graphics2D g2d = null;
        
        try {
            // まずサイズを計算
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
            
            // 実際の画像を作成
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            g2d = image.createGraphics();
            
            // 描画設定
            g2d.setFont(customFont);
            g2d.setColor(Color.WHITE);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                                RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                                RenderingHints.VALUE_RENDER_QUALITY);
            
            // ベースラインを計算
            int baseline = (int) Math.ceil(-bounds.getY());
            
            // 文字列を描画
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
    
    /**
     * BufferedImageからOpenGLテクスチャを作成
     */
    private int createTextureFromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        // ダイレクトバッファを確保（NVIDIAドライバ対応）
        int bufferSize = width * height * 4;
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(bufferSize);
        directBuffer.order(ByteOrder.nativeOrder());

        // RGBAデータをダイレクトバッファに書き込み
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            directBuffer.put((byte) ((pixel >> 16) & 0xFF));     // R
            directBuffer.put((byte) ((pixel >> 8) & 0xFF));      // G
            directBuffer.put((byte) (pixel & 0xFF));             // B
            directBuffer.put((byte) ((pixel >> 24) & 0xFF));     // A
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

        // バインド解除
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return textureId;
    }
    
    public boolean isFontLoaded() {
        return fontLoaded;
    }
    
    public String getFontPath() {
        return fontPath;
    }
    
    public void reloadFont() {
        if (fontPath != null) {
            loadFont(fontPath, fontSize);
        }
    }
    
    public void setFontSize(int size) {
        this.fontSize = size;
        reloadFont();
    }
    
    public int getFontSize() {
        return fontSize;
    }
    
    public void clearCache() {
        // OpenGLテクスチャを削除
        for (StringCacheData data : stringCache.values()) {
            if (data.textureId != -1) {
                GL11.glDeleteTextures(data.textureId);
            }
        }
        glyphCache.clear();
        stringCache.clear();
    }
    
    /**
     * リソースクリーンアップ（MOD終了時に呼び出し）
     */
    public void cleanup() {
        clearCache();
    }
}
