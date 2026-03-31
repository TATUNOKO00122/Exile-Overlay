# スキルホットバー拡張実装仕様

## 概要

既存の `SkillHotbarRenderer` に以下を追加する:
1. **ホットバー切り替え (Hotbar Swapping)** - M&Sの `HOTBAR_SWAPPING` 設定に連動
2. **召喚数表示** - 召喚系スペルのアクティブ数を右上に表示
3. **チャージインジケータ** - チャージ型スペルの残りチャージ数を左側に表示

---

## 1. ホットバー切り替え

### M&S側の仕様

- **設定**: `ClientConfigs.getConfig().HOTBAR_SWAPPING` (ForgeConfigSpec.BooleanValue, デフォルト false)
- **フラグ**: `SpellKeybind.IS_ON_SECONd_HOTBAR` (static boolean, F1でトグル)
- **OFF**: 8スロット全表示、キーは R/V/C/G + Shift+R/V/C/G
- **ON**: 前半4つか後半4つのどちらかを表示。キーは R/V/C/G のみ。F1で切り替え

### リフレクション経路 (HOTBAR_SWAPPING 設定の読み取り)

```java
// ClientConfigsのstaticフィールドアクセス
Class<?> configsClass = Class.forName("com.robertx22.mine_and_slash.config.forge.ClientConfigs");
Method getConfigMethod = configsClass.getMethod("getConfig");
Object configs = getConfigMethod.invoke(null);
Field hotbarSwapField = configsClass.getField("HOTBAR_SWAPPING");
Object hotbarSwapSpec = hotbarSwapField.get(configs);
// ForgeConfigSpec.BooleanValue.get() で boolean を取得
Method getMethod = hotbarSwapSpec.getClass().getMethod("get");
boolean hotbarSwapping = (boolean) getMethod.invoke(hotbarSwapSpec);
```

### リフレクション経路 (IS_ON_SECONd_HOTBAR フラグの読み取り)

```java
Class<?> spellKeybindClass = Class.forName("com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind");
Field isOnSecondHotbarField = spellKeybindClass.getField("IS_ON_SECONd_HOTBAR");
boolean isOnSecondHotbar = isOnSecondHotbarField.getBoolean(null);
```

### SkillHotbarRenderer への変更

#### render() メソッド内

```
ループ前に:
  hotbarSwapping = MineAndSlashHelper.isHotbarSwapping()
  isOnSecondHotbar = MineAndSlashHelper.isOnSecondHotbar()

ループ内 (slot 0-7):
  if hotbarSwapping:
    if isOnSecondHotbar && slot < 4 → skip (非アクティブ)
    if !isOnSecondHotbar && slot >= 4 → skip (非アクティブ)

  キーバインド表示:
    if hotbarSwapping:
      keyNum = isOnSecondHotbar ? slot - 4 : slot  (0-3)
      keyText = getSpellKeyText(keyNum)  // 1-4のキーのみ表示
    else:
      keyText = getSpellKeyText(slot)   // 0-7のキーを表示
```

### MineAndSlashHelper への追加メソッド

```java
// MineAndSlashHelper.java に追加

public static boolean isHotbarSwapping() {
    if (!isMnsLoaded()) return false;
    try {
        Class<?> configsClass = Class.forName("com.robertx22.mine_and_slash.config.forge.ClientConfigs");
        Method getConfig = configsClass.getMethod("getConfig");
        Object configs = getConfig.invoke(null);
        Field field = configsClass.getField("HOTBAR_SWAPPING");
        Object spec = field.get(configs);
        return (boolean) spec.getClass().getMethod("get").invoke(spec);
    } catch (Exception e) {
        LOGGER.debug("Failed to read HOTBAR_SWAPPING: {}", e.getMessage());
        return false;
    }
}

public static boolean isOnSecondHotbar() {
    if (!isMnsLoaded()) return false;
    try {
        Class<?> cls = Class.forName("com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind");
        Field field = cls.getField("IS_ON_SECONd_HOTBAR");
        return field.getBoolean(null);
    } catch (Exception e) {
        return false;
    }
}
```

---

## 2. 召喚数表示

### M&S側の仕様

- `Load.player(player).getSummonedData()` → `SummonedData` オブジェクト
- `SummonedData.getSummonedAmount(String type)` → 召喚数 (int)
- `type` には `Spell.GUID()` を渡す
- 値が 0 の場合は表示しない

### リフレクション経路

```java
// Load.player(player) → PlayerData
Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
Method playerMethod = loadClass.getMethod("player", Player.class);
Object playerData = playerMethod.invoke(null, player);

// PlayerData.getSummonedData() → SummonedData
Method getSummonedData = playerData.getClass().getMethod("getSummonedData");
Object summonedData = getSummonedData.invoke(playerData);

// SummonedData.getSummonedAmount(guid) → int
Method getSummonedAmount = summonedData.getClass().getMethod("getSummonedAmount", String.class);
int count = (int) getSummonedAmount.invoke(summonedData, spellGuid);
```

### 表示仕様

- **位置**: スロットの右上 (iconX + ICON_SIZE - 10, iconY - 2)
- **背景**: 10x10 の半透明黒背景 (0xAA000000)
- **テキスト**: 赤文字で召喚数 (ChatFormatting.RED)
- **スケール**: 0.8f
- **条件**: count > 0 の時のみ表示

### MineAndSlashHelper への追加メソッド

```java
public static int getSummonedAmount(Player player, int slot) {
    if (!isMnsLoaded()) return 0;
    try {
        String guid = getSpellGuid(player, slot);
        if (guid == null || guid.isEmpty()) return 0;

        Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
        Object playerData = loadClass.getMethod("player", Player.class).invoke(null, player);
        Object summonedData = playerData.getClass().getMethod("getSummonedData").invoke(playerData);
        return (int) summonedData.getClass()
                .getMethod("getSummonedAmount", String.class)
                .invoke(summonedData, guid);
    } catch (Exception e) {
        LOGGER.debug("Failed to get summoned amount at slot {}: {}", slot, e.getMessage());
        return 0;
    }
}
```

**注意**: `getSpellGuid()` は既存の `getHotbarSpell()` 内で `spell.GUID()` を取得しているロジックを切り出したヘルパーメソッドとして追加する。

---

## 3. チャージインジケータ

### M&S側の仕様

- **Spell.config フィールド** (`SpellConfiguration`):
  - `charges` (int): 最大チャージ数。0ならチャージ型ではない
  - `charge_regen` (int): 1チャージ回復に必要なtick数
  - `charge_name` (String): チャージの識別名

- **SpellCastingData.charges フィールド** (`ChargeData`):
  - `getCharges(String chargeName)` → 現在のチャージ数 (int)
  - `getCurrentTicksChargingOf(String chargeName)` → 次のチャージ回復までの経過tick数 (int)

- **チャージ回復の計算**:
  ```
  percent = (charge_regen - currentTicksCharging) / charge_regen
  percent = clamp(percent, 0, 1)
  ```
  - percent = 0: まだ回復開始直後
  - percent = 1: チャージ回復完了

- **表示ロジック**:
  - チャージ数 == 最大チャージ数 → `full_charges` テクスチャ
  - チャージ数 > 0 && < 最大 → `low_charges` テクスチャ
  - チャージ数 == 0 → `no_charges` テクスチャ + クールダウンオーバーレイ

### リフレクション経路

```java
// Spellオブジェクトは getHotbarSpell() で既に取得済み

// spell.config → SpellConfiguration
Field configField = spell.getClass().getField("config");
Object config = configField.get(spell);

// config.charges → int (最大チャージ数)
int maxCharges = config.getClass().getField("charges").getInt(config);
// config.charge_regen → int (回復tick数)
int chargeRegen = config.getClass().getField("charge_regen").getInt(config);
// config.charge_name → String
String chargeName = (String) config.getClass().getField("charge_name").get(config);

// Load.player(player).spellCastingData.charges → ChargeData
Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
Object playerData = loadClass.getMethod("player", Player.class).invoke(null, player);
Field spellCastingDataField = playerData.getClass().getField("spellCastingData");
Object spellCastingData = spellCastingDataField.get(playerData);
Field chargesField = spellCastingData.getClass().getField("charges");
Object chargeData = chargesField.get(spellCastingData);

// ChargeData.getCharges(chargeName) → int (現在のチャージ数)
int currentCharges = (int) chargeData.getClass()
        .getMethod("getCharges", String.class)
        .invoke(chargeData, chargeName);

// ChargeData.getCurrentTicksChargingOf(chargeName) → int
int currentTicks = (int) chargeData.getClass()
        .getMethod("getCurrentTicksChargingOf", String.class)
        .invoke(chargeData, chargeName);
```

### 表示仕様

- **位置**: スロットの左側 (slotX - 4, slotY - 4)
- **サイズ**: 20x20 のチャージインジケータテクスチャ
- **テクスチャ**: M&Sのテクスチャをそのまま使用
  - `mmorpg:textures/gui/spells/charges/full_charges.png` (満タン)
  - `mmorpg:textures/gui/spells/charges/low_charges.png` (残りあり)
  - `mmorpg:textures/gui/spells/charges/no_charges.png` (空)
- **チャージ回復中 (charges == 0)**: `no_charges` テクスチャ + スロット上にクールダウンオーバーレイ
- **条件**: `spell.config.charges > 0` のスペルのみ表示。チャージ型でないスペルは既存のクールダウン表示

### MineAndSlashHelper への追加メソッド

```java
public static class ChargeInfo {
    public final int currentCharges;
    public final int maxCharges;
    public final float regenPercent; // 0.0-1.0 次のチャージ回復進捗

    public ChargeInfo(int currentCharges, int maxCharges, float regenPercent) {
        this.currentCharges = currentCharges;
        this.maxCharges = maxCharges;
        this.regenPercent = regenPercent;
    }
}

public static ChargeInfo getSpellChargeInfo(Player player, int slot) {
    if (!isMnsLoaded()) return null;
    try {
        Object spell = getHotbarSpell(player, slot);
        if (spell == null) return null;

        Object config = spell.getClass().getField("config").get(spell);
        int maxCharges = config.getClass().getField("charges").getInt(config);
        if (maxCharges <= 0) return null; // チャージ型ではない

        int chargeRegen = config.getClass().getField("charge_regen").getInt(config);
        String chargeName = (String) config.getClass().getField("charge_name").get(config);

        Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
        Object playerData = loadClass.getMethod("player", Player.class).invoke(null, player);
        Object spellCastingData = playerData.getClass().getField("spellCastingData").get(playerData);
        Object chargeData = spellCastingData.getClass().getField("charges").get(spellCastingData);

        int currentCharges = (int) chargeData.getClass()
                .getMethod("getCharges", String.class)
                .invoke(chargeData, chargeName);

        float regenPercent = 0;
        if (currentCharges < maxCharges && chargeRegen > 0) {
            int currentTicks = (int) chargeData.getClass()
                    .getMethod("getCurrentTicksChargingOf", String.class)
                    .invoke(chargeData, chargeName);
            regenPercent = Math.min((float) currentTicks / chargeRegen, 1.0f);
        }

        return new ChargeInfo(currentCharges, maxCharges, regenPercent);
    } catch (Exception e) {
        LOGGER.debug("Failed to get charge info at slot {}: {}", slot, e.getMessage());
        return null;
    }
}
```

---

## 4. render() 内の描画順序 (最終)

各スロットの描画順序:

```
1. スペルアイコン (既存)
2. チャージ判定:
   - チャージ型スペル → チャージインジケータ (新規) + 回復中はクールダウンオーバーレイ
   - 通常スペル → クールダウンオーバーレイ (既存)
3. 召喚数 (新規、count > 0 の時のみ)
4. スロットフレーム (既存)
5. マナコスト (既存)
6. キーバインド (既存、ホットバー切り替え対応は新規)
```

---

## 5. 修正対象ファイル一覧

| ファイル | 変更内容 |
|---------|---------|
| `MineAndSlashHelper.java` | `isHotbarSwapping()`, `isOnSecondHotbar()`, `getSummonedAmount()`, `getSpellGuid()`, `getSpellChargeInfo()` 追加 |
| `SkillHotbarRenderer.java` | `render()` 内にホットバー切替ロジック、召喚数描画、チャージインジケータ描画を追加 |

---

## 6. エッジケース・注意点

- **M&S未導入時**: 全ての新規メソッドは `isMnsLoaded()` ガードあり。false を返す
- **リフレクション失敗**: 全て try-catch でラップ。LOGGER.debug で出力し、デフォルト値を返す
- **チャージ型 + 召喚型の共存**: 同じスペルが両方の性質を持つ場合、両方表示する
- **HOTBAR_SWAPPING 切替**: M&SのF1キー入力はM&S側で処理。当MODはフラグの読み取りのみ
- **getSpellGuid() の切り出し**: `getHotbarSpell()` 内の GUID 取得ロジックを共通化し、`getSummonedAmount()` と `getSpellCooldownPercent()` で再利用する
- **パフォーマンス**: リフレクション呼び出しは render ループ内で毎フレーム実行される。EntityData は既存の 250ms キャッシュを使用。PlayerData/SpellCastingData のキャッシュは不要 (スロット数が少なく、呼び出し回数が限定的なため)
