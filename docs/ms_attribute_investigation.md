# Mine and Slash 属性システム & 既存DamagePopup 調査レポート

---

## 1. M&S 属性（Stat）システム

### 1-1. 基底クラス階層

```
IGUID (Library of Exile)
  └─ JsonExileRegistry<BaseDatapackStat>
       └─ Stat (abstract)  ← 全属性の基底クラス
            ├─ IUsableStat (interface) ← 使用可能値変換（Armor等の%換算）
            ├─ ElementalStat (abstract) ← 要素別生成 + ITransferToOtherStats
            │    ├─ ElementalResist
            │    ├─ ElementalPenetration
            │    ├─ BonusFlatElementalDamage
            │    ├─ BonusPhysicalAsElemental
            │    ├─ PhysicalToElement
            │    ├─ PhysicalDamageTakenAs
            │    ├─ BonusAttackDamage
            │    └─ MaxElementalResist
            ├─ SingleElementalStat (abstract) ← 単一要素のみ生成
            ├─ 各具象Statサブクラス（防御/攻撃/リソース等）
            └─ BaseDatapackStat (datapack版Stat基底)
                 ├─ CoreStat ← ICoreStat実装
                 ├─ AttributeStat ← Vanilla属性連携
                 ├─ MoreXPerYOf ← AddToAfterCalcEnd実装
                 ├─ AddPerPercentOfOther ← AddToAfterCalcEnd実装
                 ├─ BonusStatPerEffectStacks
                 ├─ MarkerStat
                 └─ DatapackStat (DatapackStatBuilderが生成)
```

### 1-2. 基底クラス `Stat` の主要フィールド

**ファイル**: `...database/data/stats/Stat.java`

| フィールド | 型 | 説明 |
|---|---|---|
| `min` / `max` | float | 属性値の最小/最大 |
| `base` | float | ベース値 |
| `is_perc` | boolean | パーセント表記か |
| `scaling` | StatScaling | スケーリング種別 |
| `group` | StatGroup | 属性グループ(MAIN/WEAPON/CORE/ELEMENTAL/RESTORATION/Misc) |
| `gui_group` | StatGuiGroup | GUI表示グループ |
| `statEffect` | IStatEffect | この属性が発動するエフェクト |
| `icon` / `format` | String | 表示アイコン/色 |
| `has_softcap` | boolean | ソフトキャップ有無 |
| `is_long` | boolean | ロング表示形式 |
| `minus_is_good` | boolean | マイナスが良い効果か |
| `show_in_gui` | boolean | GUIに表示するか |

### 1-3. 重要インターフェース

| インターフェース | 役割 |
|---|---|
| `IUsableStat` | 属性値を実効%値に変換。`getUsableValue(Unit, int, int)` |
| `ICoreStat` | コア属性が他属性に影響。`affectStats()` / `statsThatBenefit()` |
| `ITransferToOtherStats` | Element.statを個別要素に転送 |
| `IStatEffect` | 属性効果のインターフェース |
| `IGen<T>` | 属性自動生成。`generateAndRegister()` |
| `AddToAfterCalcEnd` | 計算後に他属性へ加算 |

---

## 2. 要素（Elements）体系

### 2-1. Elements 列挙型

**ファイル**: `...uncommon/enumclasses/Elements.java`

| 列挙値 | GUID | 表示名 | 色 | 分類 | 状態異常 |
|--------|------|--------|------|------|----------|
| Physical | `physical` | Physical | GOLD | 物理 | Bleed (DOT, 100tick) |
| Fire | `fire` | Fire | RED | ELEMENTAL | Burn (DOT, 60tick) |
| Cold | `water` | Cold/Lightning | AQUA | ELEMENTAL | Freeze (蓄積→Shatter) |
| Nature | `nature` | Lightning | YELLOW | ELEMENTAL | Electrify (蓄積→Shock) |
| Shadow | `chaos` | Chaos | DARK_PURPLE | ALIGNMENT | Poison (DOT, 200tick) |
| Elemental | `elemental` | Elemental | LIGHT_PURPLE | ELEMENTAL (複合) | — |
| ALL | `all` | (全て) | LIGHT_PURPLE | 複合 | — |

### 2-2. 要素階層

```
Physical (物理)
├── 無属性, Armorで軽減, Dodgeで回避可能
│
Elemental (元素 = 3要素の複合)
├── Fire (火) → Burn (DOT)
├── Cold (水/氷) → Freeze (蓄積+Shatter)
└── Nature (雷) → Electrify (蓄積+Shock)
│
Alignment (属性)
└── Shadow/Chaos (混沌) → Poison (DOT)
```

### 2-3. 要素タグ (ElementTags)

- `PHYSICAL` — Physicalに付与
- `ELEMENTAL` — Fire/Cold/Nature + Elementalに付与
- `ALIGNMENT` — Shadowに付与

### 2-4. 要素マッチング

`elementsMatch(Elements other)` により、複合要素(Elemental, ALL)は単一要素(Fire等)にマッチする。
例: `Elemental` の耐性は `Fire`/`Cold`/`Nature` ダメージにすべて適用される。

---

## 3. 全属性リスト（カテゴリ別）

### 3-1. コア属性（Core Stats）

| GUID | 表示名 | 効果 |
|---|---|---|
| `intelligence` | Intelligence | +10 Mana, +0.5 ManaRegen, +0.5% MagicShield |
| `strength` | Strength | +5 Health, +0.25 HealthRegen, +1% Armor |
| `dexterity` | Dexterity | +10 Energy, +1% Dodge, +0.5 EnergyRegen |
| `all_attributes` | All Attributes | STR/DEX/INT全てに加算 |

### 3-2. リソース属性

| GUID | 表示名 | 備考 |
|---|---|---|
| `health` | Health | scaling: NORMAL |
| `health_regen` | Health Regen | |
| `mana` | Mana | scaling: NORMAL |
| `mana_regen` | Mana Regen | |
| `energy` | Energy | |
| `energy_regen` | Energy Regen | |
| `magic_shield` | Magic Shield | |
| `magic_shield_regen` | Magic Shield Regen | |
| `magic_shield_heal` | Magic Shield Heal | |
| `blood` | Blood | Blood Mage用 |
| `blood_user` | Blood User | Blood Mage切替 |

### 3-3. 攻撃属性（Offense）

| GUID | 表示名 | 種別 |
|---|---|---|
| `critical_hit` | Crit Chance | %, 0-100 |
| `critical_damage` | Crit Damage | %, 0-500, base=100 |
| `attack_damage` | Attack Damage | % |
| `total_damage` | Total Damage | % |
| `accuracy` | Accuracy | フラット |
| `projectile_damage` | Projectile Damage | % |
| `area_dmg` | Area Damage | % |
| `dot_dmg` | Damage Over Time | % |
| `dot_dmg_multi` | DOT Multiplier | %, 分離乗算 |
| `summon_damage` | Summon Damage | % |
| `double_attack_chance` | Double Attack Chance | % |
| `weapon_damage` | Weapon Damage | フラット |
| `spell_damage` | Skill Damage | % |

### 3-4. 防御属性（Defense）

| GUID | 表示名 | 備考 |
|---|---|---|
| `armor` | Armor | IUsableStat, 最大90%軽減 |
| `armor_penetration` | Armor Penetration | 物理, 敵Armor無視 |
| `dodge` | Dodge Rating | IUsableStat, 最大80% |
| `spell_dodge` | Spell Dodge | IUsableStat, 最大90% |
| `block_chance` | Block Chance | %, 最大75%, 50%軽減 |
| `damage_shield` | Damage Shield | フラット減算 |
| `dmg_received` | Damage Received | % |
| `dmg_reduction` | Damage Reduction | % |
| `dmg_reduction_chance` | Damage Suppression Chance | % |

### 3-5. 要素別攻撃属性（各Elements = Physical, Fire, Cold, Nature, Shadow, Elemental, ALL）

| GUIDパターン | 表示名 |
|---|---|
| `all_{element}_damage` | {Element} Damage |
| `spell_{element}_damage` | {Element} Spells Damage |
| `{element}_dot_damage` | {Element} Damage Over Time |
| `{element}_any_wep_damage` | {Element} Weapon Damage |

### 3-6. 要素別防御属性

| GUIDパターン | 表示名 |
|---|---|
| `{element}_resist` | {Element} Resistance |
| `max_{element}_resist` | Max {Element} Resistance |
| `{element}_penetration` | {Element} Penetration |
| `phys_taken_as_{element}` | Physical damage taken as {Element} |

### 3-7. Ailment（状態異常）属性

**Ailment定義**: `burn`, `poison`, `bleed`, `freeze`, `electrify`

各Ailmentごとに自動生成されるStat:

| GUIDパターン | 表示名 |
|---|---|
| `{ailment}_chance` | {Ailment} Chance |
| `{ailment}_receive_chance` | Chance to Receive {Ailment} |
| `{ailment}_damage` | {Ailment} Damage |
| `{ailment}_duration` | {Ailment} Duration |
| `{ailment}_strength` | {Ailment} Strength |
| `{ailment}_resistance` | {Ailment} Resistance |
| `{ailment}_proc_chance` | Shatter/Shock Chance |

---

## 4. 属性グループ・スケーリング

### 4-1. StatGroup

| 値 | 用途 |
|---|---|
| `MAIN` | 主要属性（Health, Armor, Crit等） |
| `WEAPON` | 武器関連属性 |
| `CORE` | コア属性（INT/STR/DEX） |
| `ELEMENTAL` | 要素属性 |
| `RESTORATION` | 回復・リーチ系 |
| `Misc` | その他 |

### 4-2. StatGuiGroup

| 値 | 用途 |
|---|---|
| `RESIST` | 要素耐性 |
| `MAX_RESIST` | 最大耐性 |
| `ELE_DAMAGE` | 要素ダメージ |
| `ELE_PENE` | 要素貫通 |
| `AILMENT_CHANCE` | 状態異常確率 |
| `AILMENT_DURATION` | 状態異常持続 |
| `AILMENT_DAMAGE` | 状態異常ダメージ |
| `AILMENT_PROC_CHANCE` | 状態異常Proc確率 |

### 4-3. StatScaling

| 値 | 用途 |
|---|---|
| `NONE` | スケーリングなし |
| `NORMAL` | 通常スケーリング |
| `CORE` | コア属性スケーリング |
| `STAT_REQ` | ステータス要件スケーリング |
| `MOB_DAMAGE` | Mobダメージスケーリング |
| `SLOW` | 低成長スケーリング |

---

## 5. ダメージ計算フロー

### 5-1. 計算優先度 (StatPriority)

```
Damage.FIRST (0)                → Crit判定
Damage.BEFORE_HIT_PREVENTION (9) → Accuracy
Damage.HIT_PREVENTION (10)       → Dodge/Block
Damage.BEFORE_DAMAGE_LAYERS (19) → Penetration/Flat追加/Resist処理前
Damage.DAMAGE_LAYERS (20)        → レイヤー式ダメージ計算
Damage.AFTER_DAMAGE_BONUSES (32) → Leech系
Damage.FINAL_DAMAGE (100)        → Proc/Ailment系
```

### 5-2. ダメージフロー全体

```
DamageEvent 発生
  ↓ 要素決定 (Elements)
  ↓ 攻撃タイプ決定 (AttackType: hit/bonus_dmg/dot)
  ↓ 武器タイプ決定 (WeaponTypes)
  ↓ プレイスタイル決定 (PlayStyle: STR/INT/DEX)
  ↓
  StatPriority.FIRST → クリティカル判定
  ↓
  StatPriority.BEFORE_HIT_PREVENTION → Accuracy適用
  ↓
  StatPriority.HIT_PREVENTION → Dodge/Block判定
  ↓
  StatPriority.BEFORE_DAMAGE_LAYERS →
    Penetration, Flat追加, 変換(Phys→Ele)
  ↓
  StatPriority.DAMAGE_LAYERS →
    Additive% ダメージ層 (TotalDmg, ElementalDmg, CritDmg等)
    More Multiplier層
    Damage Reduction層
    Damage Shield層 (フラット減算)
  ↓
  StatPriority.AFTER_DAMAGE_BONUSES → Leech系
  ↓
  StatPriority.FINAL_DAMAGE → Ailment Proc, Effect付与
```

### 5-3. AttackType

| 値 | 説明 |
|---|---|
| `hit` | 通常ヒット (isHit=true, isAttack=true) |
| `bonus_dmg` | ボーナスダメージ (isHit=true, isAttack=false) |
| `dot` | 継続ダメージ (isHit=false) |

---

## 6. 属性値の取得方法

### 6-1. エンティティからの取得

```java
EntityData data = Load.Unit(entity);
Unit unit = data.getUnit();
float value = unit.getCalculatedStat(Health.getInstance()).getValue();
```

### 6-2. ショートカット

```java
float health = unit.healthData().getValue();
float mana = unit.manaData().getValue();
float energy = unit.energyData().getValue();
float magicShield = unit.magicShieldData().getValue();
float blood = unit.bloodData().getValue();
```

### 6-3. レジストリからの取得

```java
Stat stat = ExileDB.Stats().get("health");
```

### 6-4. StatData 構造

```java
public class StatData {
    private String id;    // Stat GUID
    private float v1;     // 加算値
    private float m;      // More乗算値 (デフォルト1.0)

    public float getValue();              // v1を返す
    public float getMultiplier();         // 1 + v1/100
    public float getMoreStatTypeMulti();  // mを返す
}
```

### 6-5. StatCalculation フロー

```
1. collectStatsWithCtx() — StatContext収集
   ├── GearStats (装備)
   ├── StatusEffectStats
   ├── BaseStats
   ├── PlayerData.cachedStats
   └── MobStatUtils (Mob固有)
2. InCalcStatContainer に蓄積
3. InCalc.modify() — 転送/コア属性処理
4. ICoreStat.affectStats() — コア属性の派生効果適用
5. AddToAfterCalcEnd.affectStats() — MoreXPerYOf等の事後計算
6. softCapStat() — ソフトキャップ適用
7. AttributeStat → Vanilla属性同期
```

---

## 7. 既存 DamagePopup システム

### 7-1. クラス構成

| クラス | 行数 | 責務 |
|--------|------|------|
| `DamagePopupManager` | 502 | Singleton. 生成・マージ・更新・描画・クリーンアップの中央管理 |
| `DamageTracker` | 230 | Singleton. DPS計算・詳細追跡（**現在デッドコード**） |
| `DamageNumber` | 123 | 個別ダメージ数字の状態・アニメーション |
| `DamagePopupConfig` | 258 | Singleton. JSON設定の読み書き |
| `DamageType` | 56 | Enum. ダメージ種別定義 |
| `DamageFontRenderer` | 28 | Font描画ラッパー |
| `FontPreset` | 44 | Enum. フォントプリセット5種 |

### 7-2. DamageType 定義

```java
NORMAL(0xFFFFFF, "normal")       // Nullified/Dodge/Resist (WHITE)
PHYSICAL(0xFFAA00, "physical")   // 物理 (M&S Physical = GOLD)
FIRE(0xFF5555, "fire")           // 火・燃焼・溶岩 (M&S Fire = RED)
ICE(0x55FFFF, "ice")             // 凍結・寒冷 (M&S Cold = AQUA)
LIGHTNING(0xFFFF55, "lightning") // 雷 (M&S Lightning = YELLOW)
NATURE(0xFFFF55, "nature")       // 自然 (M&S Nature = YELLOW)
POISON(0x00FF00, "poison")       // 毒
MAGIC(0xAA00AA, "magic")         // 混沌 (M&S Shadow/Chaos = DARK_PURPLE)
ELEMENTAL(0xFF77FF, "elemental") // 複合元素 (M&S Elemental = LIGHT_PURPLE)
WITHER(0x2F2F2F, "wither")       // ウィザー
HEALING(0x55FF55, "healing")     // 回復 (M&S Heal = GREEN)
```

### 7-3. ダメージ検出パス

#### パスA: Vanilla（HP差分監視）

```
DamageMixin (@Inject TAIL on LivingEntity.setHealth)
  → DamagePopupManager.onHealthChanged(entity, newHealth)
    → diff > 0.1f → onDamage() → addDamageNumber(…, DamageType.NORMAL, …)
    → diff < -0.1f → onHeal() → addDamageNumber(…, DamageType.HEALING, …)
```

#### パスB: Mine and Slash（リフレクション介入）

```
DamageInformationMixin (@Inject HEAD on IParticleSpawnMaterial$DamageInformation.spawnOnClient)
  → リフレクションで isCrit(), getDmgMap() を取得
  → dominantElement（最大ダメージの要素）を特定
  → ci.cancel() → M&S標準パーティクルを抑制
  → DamagePopupManager.addDamageNumber(position, totalDamage, isCrit, damageType, entityId, Vec3.ZERO)
```

#### パスC: M&S ヒール検出

```
HealNumberMixin (@Inject HEAD on IParticleSpawnMaterial$HealNumber.spawnOnClient)
  → リフレクションで number() を取得
  → ci.cancel() → M&S標準パーティクルを抑制
  → DamagePopupManager.addDamageNumber(…, DamageType.HEALING, …)
```

### 7-4. 描画フロー

```
ClientTickEvent (END) → DamagePopupManager.onClientTick()
  → 全DamageNumber.tick() → 位置・速度更新
  → isExpired() → 削除

RenderLevelStageEvent (AFTER_TRANSLUCENT_BLOCKS) → DamagePopupManager.onRenderWorld()
  → computeRenderOffsets() → スクリーン空間AABB衝突解決（最大6回反復）
  → RenderState設定 (disableDepthTest, enableBlend, polygonOffset等)
  → 各DamageNumber:
      camPos相対translate → cameraOrientation()でbillboard回転
      → scale変換 → 色決定 → テキストフォーマット
      → DamageFontRenderer.renderText() (shadow + main)
  → bufferSource.endBatch()
  → RenderState復元
```

### 7-5. アニメーションパラメータ

| パラメータ | 既定値 | 挙動 |
|-----------|--------|------|
| FadeIn | 5 tick | 線形 + sin(π)バウンス |
| FadeOut | 10 tick | 線形減少 |
| 上昇 | 0.02/tick | 毎tick `*= 0.998` 減衰 |
| ノックバック | 0.06最大 | 攻撃者→被攻撃者方向, `*= 0.88` 減衰 |
| ダメージスケール | log10ベース | `1.0 + log10(damage) * 0.15`, 最大2.0倍 |
| クリティカル | 0.04f (通常0.03f) | baseScale固定倍率 |
| 表示期間 | 30 tick (1.5秒) | life >= displayDuration で期限切れ |

### 7-6. FontPreset 一覧

| 列挙値 | 表示名 | ResourceLocation |
|--------|--------|-----------------|
| `MINECRAFT` | Minecraft | `null` (デフォルトフォント) |
| `LINESEED` | Lineseed | `exile_overlay:damage_font` |
| `GAME_POCKET` | Game Pocket | `exile_overlay:damage_font_game_pocket` |
| `JERSEY_10` | Jersey 10 | `exile_overlay:damage_font_jersey_10` |
| `GOOGLE_SANS_BOLD` | Google Sans Bold | `exile_overlay:damage_font_google_sans_bold` |

---

## 8. 既存実装の問題点

### 8-1. 【重要】ダブル検出による重複ポップアップ

Vanillaパス（DamageMixin → 常に`NORMAL`型）とM&Sパス（DamageInformationMixin → エレメンタル型）が同時発火する可能性。
マージ条件は `同entityId + 同type + 同crit` だが、型が異なるためマージされず、**同一ダメージで2つのポップアップが表示される**。

### 8-2. 【中】DamageTracker がデッドコード

`recordDetailedDamage()`, `addDamage()`, `addHeal()` はどこからも呼び出されていない。
DPS計算・戦闘状態管理機能が未活用。

### 8-3. 【中】エレメンタルマッピングの不整合

3箇所に独立したマッピングが存在し、互いに異なる:

| 文字列 | DamageTracker | DamageInformationMixin | DamageType.fromDamageSource |
|--------|--------------|----------------------|---------------------------|
| "PHYSICAL" | NORMAL | — | — |
| "Fire" | (なし) | FIRE | FIRE |
| "Cold" | ICE | ICE | ICE |
| "Nature" | NATURE | NATURE | — |
| "Shadow" | MAGIC | MAGIC | — |
| "Elemental" | (なし) | MAGIC | — |

DamageTrackerは大文字ベース、DamageInformationMixinはPascalCaseで判定。

### 8-4. 【低】ノックバックのY成分除外

`calculateKnockback()` は水平方向のみ（Y=0固定）。M&Sパスでは `Vec3.ZERO` を渡すためノックバックなし。

### 8-5. 【低】ホットパスでの重複呼び出し

`DamagePopupConfig.getInstance()` が毎フレームN回呼び出される。フィールドキャッシュ or 引数渡しが望ましい。

### 8-6. 【低】`DamageType.fromDamageSource()` が未使用

Vanillaパスの `onDamage()` では常に `DamageType.NORMAL` が渡されるため、`fromDamageSource()` は実質使用されていない。

---

## 9. 主要ファイルパス一覧

### M&S 側

| 用途 | パス |
|---|---|
| Stat基底クラス | `database/data/stats/Stat.java` |
| StatGuiGroup | `database/data/stats/StatGuiGroup.java` |
| StatScaling | `database/data/stats/StatScaling.java` |
| IUsableStat | `database/data/stats/IUsableStat.java` |
| StatPriority | `database/data/stats/priority/StatPriority.java` |
| Elements列挙型 | `uncommon/enumclasses/Elements.java` |
| AttackType | `uncommon/enumclasses/AttackType.java` |
| StatData | `saveclasses/unit/StatData.java` |
| Unit | `saveclasses/unit/Unit.java` |
| Load (アクセサ) | `uncommon/datasaving/Load.java` |
| StatsRegister | `database/registrators/StatsRegister.java` |
| OffenseStats | `aoe_data/database/stats/OffenseStats.java` |
| DefenseStats | `aoe_data/database/stats/DefenseStats.java` |
| ResourceStats | `aoe_data/database/stats/ResourceStats.java` |
| DatapackStats | `aoe_data/database/stats/old/DatapackStats.java` |
| ElementalStat | `database/data/stats/types/ElementalStat.java` |
| Elements定義 | `aoe_data/database/ailments/Ailments.java` |

### exile_overlay 側

| 用途 | パス |
|---|---|
| DamagePopupManager | `forge/src/main/java/com/example/exile_overlay/client/damage/DamagePopupManager.java` |
| DamageTracker | `forge/src/main/java/com/example/exile_overlay/client/damage/DamageTracker.java` |
| DamageNumber | `forge/src/main/java/com/example/exile_overlay/client/damage/DamageNumber.java` |
| DamagePopupConfig | `forge/src/main/java/com/example/exile_overlay/client/damage/DamagePopupConfig.java` |
| DamageType | `forge/src/main/java/com/example/exile_overlay/client/damage/DamageType.java` |
| DamageFontRenderer | `forge/src/main/java/com/example/exile_overlay/client/damage/DamageFontRenderer.java` |
| FontPreset | `forge/src/main/java/com/example/exile_overlay/client/damage/FontPreset.java` |

---

## 10. クラス間依存関係

```
DamageMixin (vanilla) ──────┐
DamageInformationMixin (M&S) ├──→ DamagePopupManager ←── DamagePopupForgeHandler
HealNumberMixin (M&S) ──────┘         │
                                      ├──→ DamageNumber (状態)
                                      ├──→ DamagePopupConfig (設定)
                                      ├──→ DamageFontRenderer (描画)
                                      │        └──→ FontPreset (フォント選択)
                                      └──→ DamageType (種別)

DamageTracker (未使用) ────→ DamagePopupManager (デッドコード)
ConfigScreen ──────────────→ DamagePopupConfig (設定画面)
```
