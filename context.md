# Context: exile_overlay

## [全体概要]
Minecraft 1.20.1用MOD。Architecturyフレームワーク（Fabric + Forge）を使用。
- **Mod ID**: `exile_overlay`
- **Java**: 17
- **主機能**: HUD表示、Damageポップアップ、エフェクト管理

## [全体の現在状態]
- **フェーズ**: 開発中
- **直近の決定事項**: フォントプリセット切り替え機能完成（5種類: Minecraft標準, Jersey20, Jersey15, Jacquard12, TitanOne）
- **次ステップ**: ゲーム内で動作確認

## [ファイル・ディレクトリ別状態]

### Damageポップアップ関連 (`common/src/main/java/com/example/exile_overlay/client/damage/`)

| ファイル | 役割 | 直近の変更 |
|---------|------|-----------|
| `FontPreset.java` | フォントプリセット定義enum。MINECRAFT, JERSEY20, JERSEY15, JACQUARD12, TITAN_ONE | 新規作成 |
| `DamagePopupConfig.java` | 設定管理。`fontPreset`フィールド追加（旧`customFontPath`を置換、後方互換あり） | 更新済み |
| `DamageFontRenderer.java` | フォント描画エントリ。`reloadCustomFont()`でプリセットからロード | 更新済み |
| `CustomDamageFontRenderer.java` | TrueTypeフォント描画 | 変更なし |
| `DamagePopupManager.java` | ポップアップ管理・描画 | 変更なし |
| `DamageNumber.java` | 個別のダメージ数値データ | 変更なし |

### 設定画面 (`common/src/main/java/com/example/exile_overlay/client/config/screen/`)

| ファイル | 役割 | 直近の変更 |
|---------|------|-----------|
| `DamagePopupConfigScreen.java` | 設定UI。フォント選択サイクルボタン追加、保存時に`reloadCustomFont()`呼び出し | 更新済み |

### リソース (`common/src/main/resources/assets/exile_overlay/font/`)

| ファイル | 状態 |
|---------|------|
| `Jersey20-Regular.ttf` | デフォルトフォント |
| `Jersey15-Regular.ttf` | 新規追加 |
| `Jacquard12-Regular.ttf` | 新規追加 |
| `TitanOne-Regular.ttf` | 既存 |

### フォントプリセット一覧

| プリセット | 表示名 | リソースパス |
|-----------|--------|-------------|
| MINECRAFT | Minecraft | null (標準フォント) |
| JERSEY20 | Jersey 20 | font/Jersey20-Regular.ttf |
| JERSEY15 | Jersey 15 | font/Jersey15-Regular.ttf |
| JACQUARD12 | Jacquard 12 | font/Jacquard12-Regular.ttf |
| TITAN_ONE | Titan One | font/TitanOne-Regular.ttf |

## [直近の行動履歴]

### 2026-02-25 セッション
1. **フォントプリセット切り替え機能の実装**
   - `FontPreset` enum作成、フォントファイル2種追加（Jersey15, Jacquard12）
   - `DamagePopupConfig`: `customFontPath` → `fontPreset` に置換（後方互換性維持）
   - 設定画面にフォント選択サイクルボタン追加

2. **修正作業**
   - フォント切り替え不具合修正: 保存時に`reloadCustomFont()`呼び出し追加
   - プレビュー機能実装→ユーザー指摘で削除（重なり問題、複雑性）

### 過去セッション（要約）
- 2026-02-24: Jersey20フォント導入、スケール反映修正、デフォルト値調整
