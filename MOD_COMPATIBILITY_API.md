# MOD互換性APIドキュメント

## 概要

このMOD互換性APIは、複数のRPG MOD（Mine and Slash等）のデータを統一的に取得し、
互換性のあるMODがない場合は自動的にバニラデータにフォールバックします。

## 特徴

- **自動MOD検出**: MODがロードされているか自動的に検出
- **優先度システム**: 複数MODがある場合、優先度の高いものを自動選択
- **統一キャッシュ**: パフォーマンス最適化のためのキャッシュシステム
- **エラーハンドリング**: 安全なフォールバック機構
- **拡張性**: 新しいMOD対応が容易

## クイックスタート

### データの取得

```java
// 個別にデータを取得
float health = ModDataProviderRegistry.getCurrentHealth(player);
float mana = ModDataProviderRegistry.getCurrentMana(player);
int level = ModDataProviderRegistry.getLevel(player);

// ModDataとして一括取得
ModData data = ModDataProviderRegistry.getModData(player);
float health = data.getFloat("health.current");
float mana = data.get(DataType.CURRENT_MANA, 0.0f);
```

## 新しいMODに対応する

### 方法1: AbstractModDataProviderを継承（推奨）

```java
public class MyModProvider extends AbstractModDataProvider {
    
    private static final String MOD_ID = "my_mod";
    private static final int PRIORITY = 150;
    
    @Override
    public boolean isAvailable() {
        return checkModLoaded(MOD_ID);
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public String getId() {
        return MOD_ID;
    }
    
    @Override
    public float getCurrentMana(Player player) {
        return fetchSafely(player, DataType.CURRENT_MANA, p -> {
            return MyModAPI.getMana(p);
        });
    }
}

// 登録
ModDataProviderRegistry.register(new MyModProvider());
```

### 方法2: ModDataProviderBuilderを使用

```java
ModDataProviderBuilder.create("my_mod")
    .priority(150)
    .checkModLoaded("my_mod")
    .withMana(player -> MyModAPI.getMana(player))
    .withMaxMana(player -> MyModAPI.getMaxMana(player))
    .register();
```

### 方法3: リフレクションを使用

```java
ModDataProviderBuilder.create("my_mod")
    .priority(150)
    .checkModLoaded("my_mod")
    .withReflectionMana("com.mymod.API", "getPlayerMana")
    .withReflectionMaxMana("com.mymod.API", "getPlayerMaxMana")
    .register();
```

## APIクラス一覧

### IModDataProvider
データプロバイダーの基本インターフェース

### AbstractModDataProvider
抽象基底クラス。キャッシュ管理とエラーハンドリングを提供

### ModDataProviderRegistry
プロバイダーの管理とデータ取得のエントリーポイント

### ModDataProviderBuilder
ビルダーパターンによる簡単なプロバイダー作成

### ModData
キー・バリュー形式のデータコンテナ

### DataType
標準データタイプのenum定義

### DataTypeRegistry
カスタムデータタイプの登録管理

### ModDataCache
キャッシュ管理システム

## 優先度ガイドライン

- **200+**: アドオン/拡張MOD
- **100-199**: RPG系MOD（Mine and Slash等）
- **50-99**: ユーティリティMOD
- **0-49**: バニラ互換/フォールバック

## サンプル

詳細なサンプルは `ExampleModDataProvider.java` を参照してください。

## 対応MOD

- ✅ **Mine and Slash** - 優先度100
- ✅ **バニラ** - 優先度0（フォールバック）

## ライセンス

このAPIは内部使用のため公開されていません。
