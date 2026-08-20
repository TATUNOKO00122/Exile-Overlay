package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.DataResult;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * HUD上の物理スロットに表示されるオーブの設定を保持するクラス。
 * overlayFor: このオーブが重なるオーブ、overlayProvider: オーバーレイ用データプロバイダー。
 * ORB_1 = メイン(overlayFor=null)、ORB_1_OVERLAY = ORB_1に重なる(overlayFor="orb_1")。
 */
public class OrbConfig {
    
    private final String id;
    private final int x;
    private final int y;
    private final int size;
    private final int color;
    private final int overlayColor;
    private final boolean showReflection;
    private final ResourceLocation reflectionTexture;
    private final Float reflectionX;
    private final Float reflectionY;
    private final Float reflectionWidth;
    private final Float reflectionHeight;
    private final float renderOffsetX;
    private final float renderOffsetY;
    private final OrbDataProvider dataProvider;
    private final Predicate<Player> visibilityPredicate;
    
    // オーバーレイ関連
    private final String overlayFor;              // 重なる先のオーブID
    private final OrbDataProvider overlayProvider; // オーバーレイのデータプロバイダー
    
    private OrbConfig(Builder builder) {
        this.id = builder.id;
        this.x = builder.x;
        this.y = builder.y;
        this.size = builder.size;
        this.color = builder.color;
        this.overlayColor = builder.overlayColor;
        this.showReflection = builder.showReflection;
        this.reflectionTexture = builder.reflectionTexture;
        this.reflectionX = builder.reflectionX;
        this.reflectionY = builder.reflectionY;
        this.reflectionWidth = builder.reflectionWidth;
        this.reflectionHeight = builder.reflectionHeight;
        this.renderOffsetX = builder.renderOffsetX;
        this.renderOffsetY = builder.renderOffsetY;
        this.dataProvider = builder.dataProvider;
        this.visibilityPredicate = builder.visibilityPredicate;
        this.overlayFor = builder.overlayFor;
        this.overlayProvider = builder.overlayProvider;
    }
    
    public String getId() {
        return id;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getSize() {
        return size;
    }
    
    public int getColor() {
        return color;
    }
    
    public int getOverlayColor() {
        return overlayColor;
    }
    
    public boolean shouldShowReflection() {
        return showReflection;
    }
    
    public ResourceLocation getReflectionTexture() {
        return reflectionTexture;
    }

    public float getReflectionX() {
        return reflectionX != null ? reflectionX : getCenterX();
    }

    public float getReflectionY() {
        return reflectionY != null ? reflectionY : getCenterY();
    }

    public float getReflectionWidth() {
        return reflectionWidth != null ? reflectionWidth : size;
    }

    public float getReflectionHeight() {
        return reflectionHeight != null ? reflectionHeight : size;
    }
    
    public float getRenderOffsetX() {
        return renderOffsetX;
    }
    
    public float getRenderOffsetY() {
        return renderOffsetY;
    }
    
    public OrbDataProvider getDataProvider() {
        return dataProvider;
    }
    
    public boolean isVisible(Player player) {
        return visibilityPredicate.test(player);
    }
    
    /**
     * このオーブがオーバーレイかどうか
     * @return 他のオーブの上に重なる場合はtrue
     */
    public boolean isOverlay() {
        return overlayFor != null;
    }
    
    /**
     * 重なる先のオーブIDを取得
     * @return 重なる先のオーブID、オーバーレイでない場合はnull
     */
    public String getOverlayFor() {
        return overlayFor;
    }
    
    /**
     * オーバーレイのデータプロバイダーを取得
     * @return オーバーレイのデータプロバイダー、設定されていない場合はnull
     */
    public OrbDataProvider getOverlayProvider() {
        return overlayProvider;
    }
    
    /**
     * このオーブにオーバーレイが定義されているか
     * @return overlayColorが設定されている場合はtrue
     */
    public boolean hasOverlayColor() {
        return overlayColor != 0x00FFFFFF;
    }
    
    /**
     * 円の中心X座標を取得
     */
    public int getCenterX() {
        return x - (size / 2);
    }
    
    /**
     * 円の中心Y座標を取得
     */
    public int getCenterY() {
        return y - (size / 2);
    }
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private int x;
        private int y;
        private int size = 85;
        private int color = 0xFFFFFFFF;
        private int overlayColor = 0x00FFFFFF;
        private boolean showReflection = true;
        private ResourceLocation reflectionTexture = new ResourceLocation("exile_overlay", "textures/gui/orb_reflection.png");
        private Float reflectionX = null;
        private Float reflectionY = null;
        private Float reflectionWidth = null;
        private Float reflectionHeight = null;
        private float renderOffsetX = 0.0f;
        private float renderOffsetY = 0.0f;
        private OrbDataProvider dataProvider;
        private Predicate<Player> visibilityPredicate = p -> true;
        
        // オーバーレイ関連
        private String overlayFor = null;
        private OrbDataProvider overlayProvider = null;
        
        private Builder(String id) {
            this.id = id;
        }
        
        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }
        
        public Builder size(int size) {
            this.size = size;
            return this;
        }
        
        public Builder color(int color) {
            this.color = color;
            return this;
        }
        
        public Builder overlayColor(int overlayColor) {
            this.overlayColor = overlayColor;
            return this;
        }
        
        public Builder showReflection(boolean show) {
            this.showReflection = show;
            return this;
        }
        
        public Builder reflectionTexture(ResourceLocation texture) {
            this.reflectionTexture = texture;
            return this;
        }

        public Builder reflectionBounds(float x, float y, float width, float height) {
            this.reflectionX = x;
            this.reflectionY = y;
            this.reflectionWidth = width;
            this.reflectionHeight = height;
            return this;
        }
        
        public Builder renderOffset(float offsetX, float offsetY) {
            this.renderOffsetX = offsetX;
            this.renderOffsetY = offsetY;
            return this;
        }
        
        public Builder dataProvider(OrbDataProvider provider) {
            this.dataProvider = provider;
            return this;
        }
        
        public Builder visibleWhen(Predicate<Player> predicate) {
            this.visibilityPredicate = predicate;
            return this;
        }
        
        /**
         * このオーブがオーバーレイとして動作するよう設定
         * 
         * @param targetOrbId 重なる先のオーブID
         * @param overlayProvider オーバーレイのデータプロバイダー
         */
        public Builder asOverlayFor(String targetOrbId, OrbDataProvider overlayProvider) {
            this.overlayFor = targetOrbId;
            this.overlayProvider = overlayProvider;
            return this;
        }
        
        /**
         * このオーブにオーバーレイを定義（ORB_1のようなメインオーブ用）
         * 
         * @param overlayColor オーバーレイの色
         */
        public Builder withOverlay(int overlayColor) {
            this.overlayColor = overlayColor;
            return this;
        }
        
        /**
         * 設定を構築（バリデーション付き）
         * 
         * @return バリデーション結果付きの設定
         */
        public DataResult<OrbConfig> buildWithValidation() {
            OrbConfig config = new OrbConfig(this);
            OrbConfigValidator validator = new OrbConfigValidator();
            ValidationResult result = validator.validate(config);
            
            if (result.isValid()) {
                return DataResult.success(config);
            } else {
                return DataResult.failure(
                    String.join("; ", result.getErrors()),
                    config  // エラーがあっても設定は返す（フォールバック用）
                );
            }
        }
        
        /**
         * 設定を構築（厳格モード：エラー時は例外）
         * 
         * @return 構築された設定
         * @throws IllegalStateException バリデーションエラー時
         * @deprecated 代わりに buildWithValidation() を使用してください
         */
        @Deprecated
        public OrbConfig build() {
            DataResult<OrbConfig> result = buildWithValidation();
            if (result.isFailure()) {
                throw new IllegalStateException(
                    "Invalid OrbConfig for '" + id + "': " + result.getError()
                );
            }
            return result.getValue();
        }
    }
}
