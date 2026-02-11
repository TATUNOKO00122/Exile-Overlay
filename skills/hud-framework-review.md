---
name: HUD Framework Review
description: HUDフレームワーク実装時の設計品質チェックリスト。アーキテクチャ、パフォーマンス、堅牢性の3観点から致命的な設計ミスを防止
triggers:
  - HUD要素の新規実装時
  - データソースとレンダラーの連携実装時
  - 外部MOD連携機能の追加時
---

# Skill: HUD Framework Review

## Overview

HUDフレームワーク実装時の設計品質チェックリスト。アーキテクチャ、パフォーマンス、堅牢性の3観点から致命的な設計ミスを防止します。

## Trigger Conditions

- HUD要素の新規実装時
- データソースとレンダラーの連携実装時
- 外部MOD連携機能の追加時

## Checklist

### 1. Architecture & Coupling (アーキテクチャと結合度)

#### [ ] Dependency Inversion Check
```java
// ❌ BAD: 具象クラスへの直接依存
public class HotbarRenderer {
    private final ModDataProviderRegistry registry;  // 具象クラス
}

// ✅ GOOD: インターフェースによる抽象化
public class HotbarRenderer {
    private final IDataSource dataSource;  // インターフェース
    
    public HotbarRenderer(IDataSource source) {  // 依存性注入
        this.dataSource = source;
    }
}
```

**Checkpoint:**
- [ ] レンダラーがデータソースの具象クラスを参照していないか？
- [ ] コンストラクタまたはSetterで依存性を注入できるか？
- [ ] インターフェースが適切に定義されているか？

#### [ ] Open/Closed Principle Check
```java
// ❌ BAD: 新規要素追加時に既存クラスを修正が必要
public void render() {
    renderHealth();
    renderMana();
    // 新要素追加時にここを修正 → OCP違反
}

// ✅ GOOD: プラグイン形式で拡張可能
public void render() {
    for (IHudElement element : elements) {
        element.render(graphics);
    }
}
```

**Checkpoint:**
- [ ] 新しいHUD要素を追加する際、既存コードを修正せずに追加できるか？
- [ ] 要素の登録/解除が動的に行えるか？

#### [ ] Single Registry Principle
**Checkpoint:**
- [ ] データソースの管理は単一のレジストリで行われているか？
- [ ] `ModDataProviderRegistry`と`OrbRegistry`のような2重管理になっていないか？

---

### 2. Performance & Resource Management (パフォーマンスとリソース管理)

#### [ ] Render Loop Memory Allocation
```java
// ❌ BAD: 毎フレーム新しいインスタンス生成
public void render() {
    List<OrbType> visibleOrbs = OrbRegistry.getVisibleOrbs(player);  // 毎フレームnew ArrayList
    visibleOrbs.forEach(orb -> render(orb));  // ラムダ生成
}

// ✅ GOOD: フレーム間でインスタンスを再利用
public class RenderState {
    private final List<OrbType> visibleOrbsBuffer = new ArrayList<>();
    
    public void update(Player player) {
        visibleOrbsBuffer.clear();  // 再利用
        // ... 更新処理
    }
}

// ✅ GOOD: インデックスベースのforループ（ラムダ不使用）
for (int i = 0, n = visibleOrbs.size(); i < n; i++) {
    render(visibleOrbs.get(i));
}
```

**Checkpoint:**
- [ ] `render()`メソッド内で`new`キーワードを使用していないか？
- [ ] コレクション操作でStream/LINQを使用していないか？
- [ ] ラムダ式が毎フレーム新しいインスタンスを生成していないか？

#### [ ] Data Update Frequency
```java
// ❌ BAD: 毎フレーム不変データを取得
float maxHp = provider.getMaxValue(player);  // レベルアップ時のみ変化

// ✅ GOOD: 更新頻度に応じた間引き
public class ThrottledDataSource implements IDataSource {
    private final long updateIntervalMs = 1000;  // 1秒間隔
    private long lastUpdate = 0;
    private float cachedValue;
    
    @Override
    public float getValue(Player player) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > updateIntervalMs) {
            cachedValue = delegate.getValue(player);
            lastUpdate = now;
        }
        return cachedValue;
    }
}
```

**Checkpoint:**
- [ ] 更新頻度の低いデータ（最大HP、レベル等）を毎フレーム取得していないか？
- [ ] キャッシュ戦略が適切に実装されているか？

#### [ ] Cache Utilization
```java
// ❌ BAD: 複雑なキャッシュヒット判定
T cached = cache.getCachedData(player, dataType);
if (cached != null && !cached.equals(dataType.getDefaultValue())) {
    return cached;
}

// ✅ GOOD: Optionalまたは専用クラスでのラップ
Optional<T> cached = cache.get(player, dataType);
return cached.orElseGet(() -> fetchFreshData(player, dataType));
```

**Checkpoint:**
- [ ] キャッシュの有効期限管理が適切か？
- [ ] キャッシュミス時のデフォルト値処理が明確か？

---

### 3. Robustness & Error Handling (堅牢性とエラーハンドリング)

#### [ ] Boundary Defense
```java
// ❌ BAD: 例外による制御フロー
public OrbConfig build() {
    if (dataProvider == null) {
        throw new IllegalStateException("DataProvider must be set");
    }
    return new OrbConfig(this);
}

// ✅ GOOD: Result型によるエラーハンドリング
public class DataResult<T> {
    private final T value;
    private final String error;
    private final boolean success;
    
    public static <T> DataResult<T> success(T value) { ... }
    public static <T> DataResult<T> failure(String error, T defaultValue) { ... }
    
    public T orElse(T defaultValue) {
        return success ? value : defaultValue;
    }
}
```

**Checkpoint:**
- [ ] 外部MODから`null`や異常値が渡された際、HUD全体がクラッシュしないか？
- [ ] 要素単位で障害を隔離できるか？

#### [ ] Element-wise Failure Isolation
```java
// ✅ GOOD: 1要素の失敗で全体が崩壊しない
public class SafeHudRenderer implements IHudRenderer {
    private final Map<String, Boolean> disabledElements = new ConcurrentHashMap<>();
    
    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        String elementId = ctx.getElementId();
        if (disabledElements.containsKey(elementId)) {
            return;  // 既に無効化済み
        }
        
        try {
            delegate.render(graphics, ctx);
        } catch (Exception e) {
            LOGGER.error("HUD element {} failed, disabling", elementId, e);
            disabledElements.put(elementId, true);  // 障害隔離
        }
    }
}
```

**Checkpoint:**
- [ ] 特定のHUD要素が失敗しても、他の要素は正常に描画されるか？
- [ ] 失敗した要素は自動的に無効化されるか？

#### [ ] Configuration Validation
```java
// ✅ GOOD: 設定値のバリデーション
public class OrbConfigValidator {
    public ValidationResult validate(OrbConfig config) {
        List<String> errors = new ArrayList<>();
        
        if (config.getSize() <= 0 || config.getSize() > 256) {
            errors.add("Size must be between 1 and 256");
        }
        if (config.getX() < 0 || config.getY() < 0) {
            errors.add("Position cannot be negative");
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
}
```

**Checkpoint:**
- [ ] 不正な座標やサイズが指定された場合、デフォルト値にフォールバックするか？
- [ ] 設定ロード時にバリデーションが行われるか？

#### [ ] Logging Level Appropriateness
```java
// ❌ BAD: エラー時にDEBUGログのみ
} catch (Exception e) {
    LOGGER.debug("Error checking visibility: {}", e.getMessage());
}

// ✅ GOOD: エラー時はERRORレベル
} catch (Exception e) {
    LOGGER.error("HUD element {} failed, disabling: {}", elementId, e.getMessage(), e);
}
```

**Checkpoint:**
- [ ] エラー発生時に適切なログレベル（ERROR/WARN）が使用されているか？
- [ ] 運用時に問題を検知できるログ出力になっているか？

---

## Common Anti-Patterns

### Anti-Pattern 1: Static Registry Access
```java
// ❌ 避けるべき
float value = ModDataProviderRegistry.getValue(player, type);

// ✅ 推奨
public class HotbarRenderer {
    private final IDataSource dataSource;
    
    public HotbarRenderer(IDataSource source) {
        this.dataSource = source;
    }
}
```

### Anti-Pattern 2: Frame-by-Frame List Generation
```java
// ❌ 避けるべき
List<OrbType> orbs = OrbRegistry.getVisibleOrbs(player);  // 毎フレームnew ArrayList

// ✅ 推奨
private final List<OrbType> orbBuffer = new ArrayList<>();

public void update() {
    orbBuffer.clear();
    // ... 更新
}
```

### Anti-Pattern 3: Lambda in Hot Path
```java
// ❌ 避けるべき
orbs.forEach(orb -> render(orb));  // ラムダ生成

// ✅ 推奨
for (int i = 0; i < orbs.size(); i++) {
    render(orbs.get(i));
}
```

---

## References

- [Dependency Inversion Principle](https://en.wikipedia.org/wiki/Dependency_inversion_principle)
- [Open/Closed Principle](https://en.wikipedia.org/wiki/Open/closed_principle)
- Minecraft Render Thread Best Practices
