# 湾曲HP数値テキスト実装

## 目標
HPバーの上に湾曲したテキストを表示する（バーと同じ円弧に沿って配置）

## チェックリスト

- [x] 計画策定・承認取得
- [x] `renderCurvedText()`メソッド実装
  - 文字幅計算
  - 角度マッピング
  - 文字ごとの円弧配置
- [x] `renderHealthText()`から呼び出し
- [x] ビルド確認
- [ ] 動作確認・調整

## 設計パラメータ
| パラメータ | 値 | 説明 |
|-----------|---|------|
| textRadius | baseRadius + 0.05f * heightScale | バーより少し外側 |
| textY | barYTop + 0.03f * heightScale | バーの上端より上 |
| textArcRange | angleRange * 0.9f | バーのアークより少し狭く |
| charScale | 0.018f | 文字サイズ |

## 実装詳細

### renderCurvedText()
- 各文字を円弧上に配置
- `font.width()`で文字幅を取得
- 累積幅から角度を計算: `angle = startAngle + charCenterWidth * anglePerWidth`
- 各文字を`translate(cos, y, sin)`で位置決め
- `rotateY(-angle + 90°)`で文字が中心を向くよう回転

## 結果
- ビルド成功
- ゲーム内での視覚確認が必要
