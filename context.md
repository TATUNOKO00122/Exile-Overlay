# Context: exile_overlay

## [全体概要]
Minecraft 1.20.1用MOD。Architecturyフレームワーク（Fabric + Forge）を使用。
- **Mod ID**: `exile_overlay`
- **Java**: 17 / Gradle 8.x
- **主機能**: HUD表示、Damageポップアップ、Mob HPバー（3D）、各種情報オーバーレイ、Energy Orb表示。

## [全体の現在状態]
- **フェーズ**: HUD要素の微調整（位置・視覚的整合性）
- **決定事項**: 
  - Energy Orbの反射テクスチャと本体（液面）の位置を左に1pxずつ移動し、視覚的な整合性を改善。
- **次ステップ**: ゲーム内での表示確認。

## [ファイル・ディレクトリ別状態]

### Orb描画システム (`common/src/main/java/.../client/render/orb/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `OrbRenderer.java` | オーブ描画（Fill/Overlay層） | **[調整]** 反射テクスチャのX座標オフセットを `orbX - 1` → `orbX - 2` に変更（左に1px追加移動）。 |
| `OrbShaderRenderer.java` | GPUシェーダー方式の液面描画 | **[調整]** `OFFSET_X = -1.0f` 定数を追加し、座標計算 `adX = x - PADDING + OFFSET_X` に適用（左に1px移動）。 |

### Mob HPバー描画 (`common/src/main/java/.../client/render/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `EntityHpBarRenderer.java` | 3D HPバー＋数値描画 | ビルボード方式でテキスト描画。HPバー進行方向は右側が最大値。 |

## [直近の行動履歴]

### 2026-03-03 (最新セッション: Energy Orb位置調整)
- **反射テクスチャと本体の位置合わせ**
  - `OrbRenderer.java:99`: 反射テクスチャのX座標を `orbX - 2` に変更
  - `OrbShaderRenderer.java:74,107`: `OFFSET_X = -1.0f` 定数を追加し、液面描画位置を左に1px移動
  - ビルド確認完了

### 過去セッション（極度圧縮）
- **2026-02-26**: HPバーのテキスト消失・反転バグを解決（ビルボード方式化）。HPバー進行方向を修正。
