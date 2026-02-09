# SKILL: Mod Integration Abstraction Pattern

## 概要
外部MOD（Mine and Slash等）との連携を行う際の設計パターン。強い結合を避け、拡張性と保守性を確保するための標準的なアプローチ。

## トリガー条件
- 外部MODのデータを取得・表示する機能を追加する場合
- リフレクションを使用して外部APIにアクセスする場合
- 複数のMODに対応する必要がある場合

## 適用パターン

### 1. インターフェース定義
```java
public interface IModDataProvider {
    boolean isAvailable();                    // MODがロードされているか
    int getPriority();                        // 優先度（高いほど優先）
    String getId();                           // プロバイダーID
    
    // データ取得メソッド
    float getCurrentHealth(Player player);
    float getMaxHealth(Player player);
    // ... その他必要なメソッド
}
```

### 2. レジストリ実装
```java
public class ModDataProviderRegistry {
    private static final List<IModDataProvider> providers = new ArrayList<>();
    private static IModDataProvider activeProvider = null;
    
    public static void register(IModDataProvider provider) {
        providers.add(provider);
        providers.sort(Comparator.comparingInt(IModDataProvider::getPriority).reversed());
        updateActiveProvider();
    }
    
    private static void updateActiveProvider() {
        for (IModDataProvider provider : providers) {
            if (provider.isAvailable()) {
                activeProvider = provider;
                break;
            }
        }
    }
    
    // ヘルパーメソッド
    public static float getCurrentHealth(Player player) {
        return getActiveProvider().getCurrentHealth(player);
    }
}
```

### 3. 各MOD用実装
```java
public class ExampleModDataProvider implements IModDataProvider {
    private static final int PRIORITY = 100;
    
    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.example.mod.MainClass");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public float getCurrentHealth(Player player) {
        // 外部MODのAPIを使用
        // 失敗時はバニラの値を返す
        try {
            return ExternalModAPI.getHealth(player);
        } catch (Exception e) {
            return player.getHealth();
        }
    }
}
```

### 4. バニラフォールバック
```java
public class VanillaDataProvider implements IModDataProvider {
    @Override
    public boolean isAvailable() {
        return true; // 常に利用可能
    }
    
    @Override
    public int getPriority() {
        return 0; // 最低優先度
    }
    
    @Override
    public float getCurrentHealth(Player player) {
        return player.getHealth();
    }
}
```

## 初期化
```java
public class ExampleMod {
    public static void init() {
        // バニラプロバイダー（フォールバック用）
        ModDataProviderRegistry.register(new VanillaDataProvider());
        
        // 各MOD用プロバイダー
        ModDataProviderRegistry.register(new MineAndSlashDataProvider());
        ModDataProviderRegistry.register(new ArsNouveauDataProvider());
    }
}
```

## 利点
1. **疎結合**: 外部MODがなくても動作する
2. **拡張性**: 新しいMOD対応を追加しやすい
3. **優先度制御**: 複数MODがある場合の優先順位を設定可能
4. **テスト容易性**: モックプロバイダーで単体テスト可能

## 注意点
- `isAvailable()`は軽量に実装すること
- 例外処理は必ず行い、デフォルト値を返す
- リフレクション結果は`MethodHandle`でキャッシュすること

## 関連ファイル
- `IModDataProvider.java`
- `ModDataProviderRegistry.java`
- `VanillaDataProvider.java`
- `MineAndSlashDataProvider.java`
