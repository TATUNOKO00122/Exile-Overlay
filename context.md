# Context: exile_overlay

## [全体概要]
Minecraft 1.20.1用MOD。Architecturyフレームワーク（Fabric + Forge）を使用。
- **Mod ID**: `exile_overlay`
- **Java**: 17 / Gradle 8.x
- **主機能**: HUD表示、Damageポップアップ、各種情報オーバーレイ、Energy Orb表示、ターゲット情報表示（レベル+名前+HP）、スキルホットバー（クールダウン表示）
- **外部MOD連携**: Mine-And-Slash-Rework（Forge専用）のMobレベル表示、スキル情報取得に対応
- **無効化機能**: 3D Mob HPバー（コメントアウト済み）

## [全体の現在状態]
- **フェーズ**: 安定運用中
- **決定事項**: 
  - 3D Mob HPバー機能を完全にコメントアウト（UI設定ボタン含む）
  - ターゲット情報HUDにMine-And-Slash-Reworkのレベル表示を追加済み
  - スキルクールダウン表示のバグを修正（リフレクション型不一致）
- **次ステップ**: 特になし

## [ファイル・ディレクトリ別状態]

### Mine-And-Slash連携 (`common/src/main/java/.../util/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `MineAndSlashHelper.java` | M&S MODデータ取得ヘルパー | **[修正済み]** `Load.Unit(Entity.class)` に修正（元は `Player.class` でNoSuchMethodException発生）。クールダウン取得メソッドにデバッグログ追加。 |

### スキルホットバー (`common/src/main/java/.../client/render/skill/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `SkillHotbarRenderer.java` | スキルホットバー描画 | クールダウン表示機能あり（オーバーレイ、秒数表示、境界線）。変更なし。 |

### 3D Mob HPバー（無効化済み）
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `EntityHpBarRenderer.java` | 3D HPバー描画 | **[無効化]** クラス全体をコメントアウト |
| `EntityHpBarFabricHandler.java` | Fabric用イベントハンドラ | **[無効化]** register()本体・importをコメントアウト |
| `EntityHpBarForgeHandler.java` | Forge用イベントハンドラ | **[無効化]** onRenderLevelStage()本体・importをコメントアウト |
| `MobHealthBarConfig.java` | 3D HPバー設定クラス | **[無効化]** クラス全体をコメントアウト |
| `MobHealthBarConfigScreen.java` | 3D HPバー設定画面 | **[無効化]** クラス全体をコメントアウト |
| `HudListScreen.java` | HUD設定一覧画面 | **[無効化]** mobHealthBarButton関連をコメントアウト、ボタン位置を詰め |

### ターゲット情報HUD (`common/src/main/java/.../client/render/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `TargetInfoRenderer.java` | カーゲット情報表示（レベル+名前+HP） | Mine-And-Slashレベル表示対応。表示形式: `Lv.15 Zombie` |

### HUD管理 (`common/src/main/java/.../client/render/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `HudRenderManager.java` | HUDレンダラー登録管理 | TargetInfoRenderer、SkillHotbarRendererへの参照。変更なし。 |

### Orb描画システム (`common/src/main/java/.../client/render/orb/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `OrbRenderer.java` | オーブ描画（Fill/Overlay層） | 反射テクスチャ位置調整済み。 |
| `OrbShaderRenderer.java` | GPUシェーダー方式の液面描画 | 液面位置調整済み。 |

## [直近の行動履歴]

### 2026-03-04 (スキルクールダウン表示バグ修正)
- **目的**: スキルホットバーのクールダウンが表示されない問題を修正
- **調査**:
  - Mine and Slash 6.3.14のJARをデコンパイルしてAPI構造を確認
  - ログ解析で `data=null` が返されていることを特定
  - HJUD MOD（正常動作）と比較してリフレクション使用方法の違いを発見
- **原因**: `Load.Unit(Player.class)` → `NoSuchMethodException`（正しいシグネチャは `Load.Unit(Entity.class)`）
- **修正内容**:
  - `MineAndSlashHelper.java`: `Entity.class` import追加、`getMethod("Unit", Entity.class)` に修正
  - デバッグログ追加（クールダウン値、例外内容を出力）
- **ビルド**: 成功確認

### 過去セッション（極度圧縮）
- **2026-03-03**: 3D Mob HPバー無効化（6ファイルコメントアウト、翻訳キー削除）。
- **2026-03-03**: Mine-And-Slash-Rework連携（TargetInfoRendererレベル表示追加）。
- **2026-03-03**: ターゲットMob HUD刷新（テクスチャ、プログレスバー統合）。
- **2026-03-03**: Energy Orb位置調整。
- **2026-02-26**: HPバーテキスト消失・反転バグ解決。
