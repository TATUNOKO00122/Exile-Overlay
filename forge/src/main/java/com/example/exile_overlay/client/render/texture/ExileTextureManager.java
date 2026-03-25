package com.example.exile_overlay.client.render.texture;

import com.example.exile_overlay.client.render.orb.OrbShaderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * カスタムテクスチャとシェーダーの初期化管理クラス
 *
 * 【役割】
 * - GPUレンダリング用テクスチャのロード確認
 * - カスタムシェーダーの初期化
 * - フォールバック制御
 */
public class ExileTextureManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 必要なテクスチャ一覧
    private static final ResourceLocation[] REQUIRED_TEXTURES = {
            new ResourceLocation("exile_overlay", "textures/gui/liquid_fill.png")
    };
    
    /**
     * テクスチャとシェーダーの初期化
     * クライアント初期化時に呼び出されることを想定
     */
    public static void initialize() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            LOGGER.warn("Cannot initialize textures: Minecraft instance is null");
            return;
        }
        
        ResourceManager resourceManager = mc.getResourceManager();
        
        // テクスチャの可用性を確認
        boolean allTexturesAvailable = true;
        for (ResourceLocation texture : REQUIRED_TEXTURES) {
            boolean exists = resourceManager.getResource(texture).isPresent();
            if (!exists) {
                LOGGER.warn("Required texture not found: {}", texture);
                allTexturesAvailable = false;
            } else {
                LOGGER.debug("Texture found: {}", texture);
            }
        }
        
        // シェーダーを初期化
        OrbShaderRenderer.initializeShader();
        boolean shaderAvailable = OrbShaderRenderer.isShaderAvailable();
        
        if (allTexturesAvailable) {
            LOGGER.info("GPU rendering textures initialized successfully");
        } else {
            LOGGER.warn("Some textures are missing. Using fallback rendering.");
        }
        
        if (shaderAvailable) {
            LOGGER.info("Custom shader (orb_liquid) loaded successfully");
        } else {
            LOGGER.info("Custom shader not available. Using texture-based fallback.");
        }
    }
    
    /**
     * 特定のテクスチャが利用可能かチェック
     */
    public static boolean isTextureAvailable(ResourceLocation texture) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        
        return mc.getResourceManager().getResource(texture).isPresent();
    }
    
    /**
     * 全必要テクスチャが利用可能かチェック
     */
    public static boolean areAllTexturesAvailable() {
        for (ResourceLocation texture : REQUIRED_TEXTURES) {
            if (!isTextureAvailable(texture)) {
                return false;
            }
        }
        return true;
    }
}
