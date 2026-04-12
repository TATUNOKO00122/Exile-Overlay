[1.7.1] - 2026-04-11

### Damage Popup (ダメージポップアップ)
- Fixed billboard rotation to display correctly from all camera angles
- Removed back-face culling that caused popups to disappear when viewed from behind

### Other
- Added MIT LICENSE
- Cleaned up dev-only files from repository

[1.7.0] - 2026-04-11

### Target Info (ターゲット情報HUD)
- Added Mine and Slash mob info display to target HUD (affixes, stats, effects)
- Added TopDownView MOD integration for mouse-cursor target acquisition
- Added MOBEffect icon frame and background texture to target info display
- Added mob affix stat detail display with HP bar adjustments
- Added target info visibility toggle settings
- Fixed target info HUD position shift when buff display is added
- Fixed Mine and Slash affix display and effect rendering
- Fixed mob status effect display with vanilla effects
- Removed vanilla potion effects from target display
- Fixed HP text position (moved to bar top-right) and restored name/level positions
- Fixed affix stat display position and HP text fine-tuning

### HUD Font (カスタムフォント)
- Added custom font (Google Sans) to all HUD text elements
- Applied custom font to all HUD renderers, removed HudFontManager dead code
- Fixed HUD font CJK garbled text by switching Google Sans Bold → Medium with size adjustment

### Orb Text (オーブ数値表示)
- Redesigned orb number display: removed POE2 style, redesigned compact notation and font size settings
- Changed orb text size setting from cycle button to slider
- Changed orb compact notation to show one decimal place (e.g. 1500 → 1.5k)
- Enhanced HUD font infrastructure and config UI reorganization

### Level Display (レベル表示)
- Added level number display 3-mode toggle (hidden / simple / detailed)

### Damage Popup (ダメージポップアップ)
- Unified damage popup font rendering to JSON TTF provider method
- Fixed critical damage popup bugs and improved architecture

### Config Screen (設定画面)
- Restructured config screen from 4 tabs to 5 tabs (General, UI, Damage Popup, HP Bar, Integration)
- Moved target info, equipment, level display, and orb settings to new "UI" tab
- Moved Quick Loot, Mine and Slash, Dungeon Realm, and Inventory Sorter settings to new "Integration" tab
- Reordered Integration sections: Mine and Slash first, then Quick Loot, Dungeon Realm, Inventory Sorter
- Removed redundant "連携" from section names
- Renamed "装備HUD" section to "装備"
- Changed "HP/MP" labels to "HP/マナ" in orb text settings
- Changed default: Quick Loot OFF
- Changed default: Mine and Slash HP Bar cancellation OFF
- Changed default: Entity HP Bar OFF

### Mine and Slash Integration
- Added per-overlay-element cancel on/off toggle for Mine and Slash overlay elements

### Bug Fixes
- Fixed food gauge hit detection to match render size, correcting HUD editor hit-test bounds
- Commented out Harvest map (dungeon_hud) default HUD position registration

[1.6.2] - 2026-04-08

Changes
- Removed Lock feature entirely
- Raised HUD scale maximum from 2.0 to 4.0
- Removed unused hotbar renderer files

[1.6.1] - 2026-04-04

Changes
- Added keybind hide-when-unassigned for skill hotbar
- Added hotbar page switching support

[1.6.0] - 2026-04-01

Changes
- Added damage number rounding and compact notation options (e.g. 1.5k, 2.3M)
- Fixed M&S default popup not being cancelled when custom damage popup is disabled
