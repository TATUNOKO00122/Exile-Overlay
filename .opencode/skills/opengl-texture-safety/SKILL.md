---
name: OpenGL Texture Safety
description: OpenGLテクスチャ操作におけるNVIDIAドライバクラッシュを防止するベストプラクティス
triggers:
  - OpenGLテクスチャを毎フレーム生成・削除するコード
  - ByteBuffer.wrap()で非ダイレクトバッファを使用するコード
  - glTexImage2Dにヒープバッファを渡すコード
  - テクスチャリソースの解放を忘れているコード
---

# OpenGL Texture Safety

## 概要

Minecraft ModdingにおけるOpenGLテクスチャ操作でよく発生する**EXCEPTION_ACCESS_VIOLATION**（GPUドライバクラッシュ）を防止するためのガイドラインです。

## 問題の背景

### 発生するクラッシュ
- **EXCEPTION_ACCESS_VIOLATION**: NVIDIAドライバでのメモリアクセス違反
- **原因**: 
  1. 毎フレームのテクスチャ生成・削除
  2. 非ダイレクトバッファ（ヒープメモリ）の使用
  3. 不適切なリソース管理

## ベストプラクティス

### 1. テクスチャのキャッシュ化

**禁止（毎フレーム生成・削除）:**
```java
private void render() {
    int textureId = GL11.glGenTextures();  // ← 毎フレーム生成
    // ... 描画 ...
    GL11.glDeleteTextures(textureId);       // ← 毎フレーム削除
}
```

**推奨（キャッシュ化）:**
```java
public class TextureCache {
    private final Map<String, TextureData> cache = new HashMap<>();
    
    private static class TextureData {
        final int textureId;
        final int width;
        final int height;
        
        TextureData(int textureId, int width, int height) {
            this.textureId = textureId;
            this.width = width;
            this.height = height;
        }
    }
    
    public int getTexture(String key, BufferedImage image) {
        TextureData cached = cache.get(key);
        if (cached != null) {
            return cached.textureId;
        }
        
        int textureId = createTexture(image);
        cache.put(key, new TextureData(textureId, image.getWidth(), image.getHeight()));
        return textureId;
    }
    
    public void cleanup() {
        for (TextureData data : cache.values()) {
            GL11.glDeleteTextures(data.textureId);
        }
        cache.clear();
    }
}
```

### 2. ダイレクトバッファの使用

**禁止（非ダイレクトバッファ）:**
```java
byte[] rgbaData = new byte[width * height * 4];
// ... データ書き込み ...
GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                 width, height, 0,
                 GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                 java.nio.ByteBuffer.wrap(rgbaData));  // ← 禁止
```

**推奨（ダイレクトバッファ）:**
```java
ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4);
directBuffer.order(ByteOrder.nativeOrder());

// ... データ書き込み ...
for (int i = 0; i < pixels.length; i++) {
    int pixel = pixels[i];
    directBuffer.put((byte) ((pixel >> 16) & 0xFF));  // R
    directBuffer.put((byte) ((pixel >> 8) & 0xFF));   // G
    directBuffer.put((byte) (pixel & 0xFF));          // B
    directBuffer.put((byte) ((pixel >> 24) & 0xFF));  // A
}
directBuffer.flip();

GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                 width, height, 0,
                 GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                 directBuffer);  // ← ダイレクトバッファ
```

### 3. 適切なリソース管理

```java
public class SafeTextureManager {
    private final List<Integer> textureIds = new ArrayList<>();
    
    public int createTexture(BufferedImage image) {
        int textureId = GL11.glGenTextures();
        if (textureId == 0) {
            LOGGER.error("Failed to generate OpenGL texture");
            return -1;
        }
        
        textureIds.add(textureId);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        // ... テクスチャ設定 ...
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);  // バインド解除
        
        return textureId;
    }
    
    public void deleteTexture(int textureId) {
        if (textureIds.remove((Integer) textureId)) {
            GL11.glDeleteTextures(textureId);
        }
    }
    
    public void cleanup() {
        for (int textureId : textureIds) {
            GL11.glDeleteTextures(textureId);
        }
        textureIds.clear();
    }
}
```

### 4. エラーハンドリング

```java
private int createTextureFromImage(BufferedImage image) {
    int textureId = GL11.glGenTextures();
    if (textureId == 0) {
        LOGGER.error("Failed to generate OpenGL texture");
        return -1;
    }
    
    try {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        // ... テクスチャ設定 ...
        return textureId;
    } catch (Exception e) {
        LOGGER.error("Failed to create texture", e);
        GL11.glDeleteTextures(textureId);
        return -1;
    } finally {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
```

## チェックリスト

- [ ] テクスチャを毎フレーム生成・削除していないか
- [ ] ByteBuffer.allocateDirect()を使用しているか
- [ ] テクスチャ使用後にバインド解除（glBindTexture(0)）しているか
- [ ] MOD終了時や画面遷移時にテクスチャを解放しているか
- [ ] キャッシュサイズに上限を設け、古いテクスチャを削除しているか
- [ ] glGenTextures()の戻り値（0は失敗）をチェックしているか

## よくあるミス

### ミス1: 非ダイレクトバッファの使用
NVIDIAドライバはヒープメモリ（非ダイレクトバッファ）へのDMAをサポートしていない場合があり、クラッシュを引き起こします。

### ミス2: テクスチャリーク
キャッシュクリア時にOpenGLテクスチャを削除し忘れると、GPUメモリリークが発生します。

### ミス3: バインドの継続
テクスチャ操作後にバインドを解除しないと、後続の描画で意図しないテクスチャが使用される可能性があります。

## 関連ドキュメント

- [OpenGL Wiki - Buffer Objects](https://www.khronos.org/opengl/wiki/Buffer_Object)
- [LWJGL Documentation](https://javadoc.lwjgl.org/)
- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)
