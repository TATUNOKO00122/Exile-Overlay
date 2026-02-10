# MethodHandles型安全性スキル

## 概要
MethodHandlesを使用したリフレクションアクセス時の型安全性に関するガイドライン

## 問題
`MethodHandle.invokeExact()`は戻り値の型が厳格に一致する必要があり、
対象メソッドの戻り値型が予期しない場合（例：MODのバージョン変更など）に
`WrongMethodTypeException`が発生し、常にデフォルト値（0や1）が返される。

## 影響を受ける典型的なケース
- MOD連携時のデータ取得（体力、マナなど）
- 複数バージョン対応が必要なコード
- サードパーティライブラリのメソッド呼び出し

## 悪い例（問題あり）
```java
// 戻り値がintやdoubleの場合、floatへのキャストで失敗
public static float getValue(Entity entity) throws Throwable {
    return (float) GET_VALUE.invokeExact(entity);  // ← 危険
}
```

## 良い例（推奨）
```java
// Numberインターフェースを介して型安全に変換
public static float getValue(Entity entity) throws Throwable {
    if (GET_VALUE == null || entity == null) return 0f;
    Object result = GET_VALUE.invoke(entity);  // ← invokeを使用
    return result instanceof Number ? ((Number) result).floatValue() : 0f;
}
```

## チェックリスト
- [ ] `invokeExact`を使用している箇所を検索
- [ ] 戻り値型が厳密に一致しない可能性がある場合は`invoke`に変更
- [ ] `instanceof Number`チェックを追加
- [ ] nullチェックを適切に実装

## 例外
以下の場合は`invokeExact`を使用可能：
- 戻り値型が確実に予測可能（標準Java APIなど）
- パフォーマンスが極度に重要な箇所かつ型が保証されている場合
