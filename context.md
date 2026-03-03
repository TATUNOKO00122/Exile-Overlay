# Context: exile_overlay

## [全体概要]
Minecraft 1.20.1用MOD。Architecturyフレームワーク（Fabric + Forge）を使用。
- **Mod ID**: `exile_overlay`
- **Java**: 17 / Gradle 8.x
- **主機能**: HUD表示、Damageポップアップ、Mob HPバー（3D）、各種情報オーバーレイ、Energy Orb表示、ターゲットMob情報表示。

## [全体の現在状態]
- **フェーズ**: ターゲットMob HUD（HPバー+名前表示）の実装完了
- **決定事項**: 
  - ターゲットMobのHUD表示を名前のみから「HPバー+名前」に刷新
  - テクスチャ: `target_hp_bar_frame.png` (224×32px)
  - プログレスバー: 赤色固定、Y=20-30（高さ10px）、X=5-217（幅212px）
  - 名前表示: バーと同じ高さ（Y=22）の中央揃え
  - ターゲット保持: カーソル離脱後1秒間表示維持（WeakReference + isAliveチェック）
- **次ステップ**: ゲーム内での表示確認・HP数値表示の再有効化検討

## [ファイル・ディレクトリ別状態]

### ターゲットMob HUD (`common/src/main/java/.../client/render/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `TargetMobNameRenderer.java` | カーソル対象MobのHPバー+名前表示 | **[全面書き換え]** テクスチャ描画、プログレスバー（赤色）、名前表示を統合。スケール対応をポーズスタックで統一。ターゲット保持機能（1秒）実装。HP数値はコメントアウト中。 |

### テクスチャリソース (`common/src/main/resources/assets/exile_overlay/textures/gui/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `target_hp_bar_frame.png` | ターゲットMob HPバー枠 | **[追加]** 旧`HPバー１.png`をリネーム。224×32px。 |

### Orb描画システム (`common/src/main/java/.../client/render/orb/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `OrbRenderer.java` | オーブ描画（Fill/Overlay層） | 反射テクスチャ位置調整済み（左に2px移動）。 |
| `OrbShaderRenderer.java` | GPUシェーダー方式の液面描画 | 液面位置調整済み（左に1px移動）。 |

### Mob HPバー描画 (`common/src/main/java/.../client/render/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `EntityHpBarRenderer.java` | 3D HPバー＋数値描画 | ビルボード方式でテキスト描画。HPバー進行方向は右側が最大値。 |

## [直近の行動履歴]

### 2026-03-03 (最新セッション: ターゲットMob HUD刷新)
- **TargetMobNameRenderer全面書き換え**
  - テクスチャリネーム: `HPバー１.png` → `target_hp_bar_frame.png`
  - プログレスバー実装: 赤色固定、位置Y=20-30（高さ10px）、X=5-217（幅212px）
  - 名前表示: バーと同じ高さ（Y=22）の中央揃えに変更
  - HP数値: コメントアウト（一時無効化）
  - ターゲット保持機能: WeakReferenceで1秒間保持、死亡時は即座に非表示
  - スケール問題修正: ポーズスタックで全要素（テクスチャ・バー・テキスト）のスケールを統一
  - ビルド確認完了

### 過去セッション（極度圧縮）
- **2026-03-03**: Energy Orb位置調整（反射・液面を左に移動）。
- **2026-02-26**: HPバーのテキスト消失・反転バグを解決（ビルボード方式化）。HPバー進行方向を修正。
