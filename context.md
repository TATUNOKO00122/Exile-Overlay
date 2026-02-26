# Context: exile_overlay

## [全体概要]
Minecraft 1.20.1用MOD。Architecturyフレームワーク（Fabric + Forge）を使用。
- **Mod ID**: `exile_overlay`
- **Java**: 17
- **主機能**: HUD表示、Damageポップアップ表示、Mob HPバー、各種情報オーバーレイの提供

## [全体の現在状態]
- **フェーズ**: 開発中・Mob HPバー機能拡充
- **直近の決定事項**: 湾曲HP数値テキスト実装完了。X軸180度回転でプレイヤー方向を向くよう修正。
- **次ステップ**: ゲーム内での湾曲テキスト動作確認（位置・サイズ・可読性）

## [ファイル・ディレクトリ別状態]

### Mob HPバー描画 (`common/src/main/java/.../client/render/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `EntityHpBarRenderer.java` | 3D湾曲HPバー＋HP数値描画 | `renderCurvedText()`追加。各文字を円弧上に配置しX軸180度回転でプレイヤー方向を向かせる。パラメータ: textRadius=baseRadius+0.05f*heightScale, charScale=0.018f。 |

### Mob HPバー設定 (`common/src/main/java/.../client/config/`)
| ファイル | 役割 | 直近の変更内容・状態 |
|---------|------|------------------|
| `MobHealthBarConfig.java` | 設定クラス | 変更なし。`showHealthValue`/`showMaxHealth`/`showPercentage`のgetter使用。 |
| `MobHealthBarConfigScreen.java` | 設定画面 | 変更なし。チェックボックス既存。 |

## [直近の行動履歴]

### 2026-02-26 (最新セッション)
- **湾曲HP数値テキストの実装と調整**
  - `renderCurvedText()`新規追加: 各文字を円弧上に配置
  - 描画フロー: `translate(cos, y, sin)` → `rotateY(-angle+90°)` → `scale(charScale, -charScale, charScale)` → `rotateX(180°)`
  - トラブルシューティング:
    - 裏面描画問題 → 裏面描画不要と判断（表面のみ描画）
    - テキストがプレイヤー方向を向かない → X軸180度回転を追加
  - ビルド成功、動作確認待ち

### 過去セッション（圧縮ログ）
- **2026-02-25**: HP数値表示の初版実装（平面上描画）
- **スナップ計算バグ修正**: expansion領域を考慮
- **Mine and Slash互換性**: NeatConfig.draw無効化
- **フォント機能拡充**: フォントプリセット機能追加

## [未実装機能メモ]
Mob HPバーで設定あり・実装なしの機能:
- `showOnlyWhenDamaged` - ダメージ時のみ表示
- `showForAllMobs` - 全Mob表示制御
- `showThroughWalls` - 壁越し表示制御
- 色設定UI - backgroundColor/borderColor/healthColor等
