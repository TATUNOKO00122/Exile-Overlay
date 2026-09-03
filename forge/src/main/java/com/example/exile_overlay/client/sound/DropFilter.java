package com.example.exile_overlay.client.sound;

import com.example.exile_overlay.util.DropItemResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DropFilter {

    public record SoundRule(String sound, float volume, boolean enabled) {}

    public static class FilterSection {
        private final String sound;
        private final float volume;
        private final boolean isMute;
        private final Set<String> targets = new HashSet<>();
        private final Set<String> excludes = new HashSet<>();

        public FilterSection(String sound, float volume) {
            this.sound = sound.trim();
            this.volume = Math.max(0.0f, Math.min(2.0f, volume));
            String lower = this.sound.toLowerCase(Locale.ROOT);
            this.isMute = lower.equals("none") || lower.equals("mute") || this.volume <= 0.0001f;
        }

        public void addTarget(String target) {
            if (target.startsWith("!")) {
                excludes.add(target.substring(1).trim().toLowerCase(Locale.ROOT));
            } else {
                targets.add(target.trim().toLowerCase(Locale.ROOT));
            }
        }

        public boolean matches(DropItemResolver.ItemInfo info) {
            if (info == null) return false;

            String itemId = info.itemId() != null ? info.itemId().toLowerCase(Locale.ROOT) : "";
            String uniqueId = info.uniqueId() != null ? info.uniqueId().toLowerCase(Locale.ROOT) : null;
            String uniqueWithPrefix = uniqueId != null ? "mmorpg:unique/" + uniqueId : null;
            String rawRarity = info.rarity() != null ? info.rarity().toLowerCase(Locale.ROOT) : null;
            String rarity = rawRarity != null ? "rarity:" + rawRarity : null;

            // itemId@rarity 形式のキー (例: mmorpg:omen@legendary)
            String itemWithRarity = (!itemId.isEmpty() && rawRarity != null) ? itemId + "@" + rawRarity : null;
            String uniqueWithRarity = (uniqueId != null && rawRarity != null) ? uniqueId + "@" + rawRarity : null;
            String uniquePrefixWithRarity = (uniqueWithPrefix != null && rawRarity != null) ? uniqueWithPrefix + "@" + rawRarity : null;

            // 1. 個別除外 (!) チェック
            if (!excludes.isEmpty()) {
                if (!itemId.isEmpty() && excludes.contains(itemId)) return false;
                if (itemWithRarity != null && excludes.contains(itemWithRarity)) return false;
                if (uniqueId != null && (excludes.contains(uniqueId) || excludes.contains(uniqueWithPrefix))) return false;
                if (uniqueWithRarity != null && excludes.contains(uniqueWithRarity)) return false;
                if (uniquePrefixWithRarity != null && excludes.contains(uniquePrefixWithRarity)) return false;
                if (rarity != null && excludes.contains(rarity)) return false;
            }

            // 2. ターゲット一致チェック (より具体的な itemId@rarity も判定)
            if (itemWithRarity != null && targets.contains(itemWithRarity)) {
                return true;
            }
            if (uniqueWithRarity != null && targets.contains(uniqueWithRarity)) {
                return true;
            }
            if (uniquePrefixWithRarity != null && targets.contains(uniquePrefixWithRarity)) {
                return true;
            }
            if (!itemId.isEmpty() && targets.contains(itemId)) {
                return true;
            }
            if (uniqueId != null && (targets.contains(uniqueId) || targets.contains(uniqueWithPrefix))) {
                return true;
            }
            if (rarity != null && targets.contains(rarity)) {
                return true;
            }

            return false;
        }

        public SoundRule toRule() {
            if (isMute) {
                return new SoundRule("", 0.0f, false);
            }
            return new SoundRule(sound, volume, true);
        }
    }

    private String name = "Default Filter";
    private final List<FilterSection> sections = new ArrayList<>();

    public DropFilter() {
    }

    public DropFilter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FilterSection> getSections() {
        return sections;
    }

    public SoundRule match(DropItemResolver.ItemInfo info) {
        if (info == null) {
            return null;
        }

        // 上のセクションから順に評価 (First Match)
        for (FilterSection section : sections) {
            if (section.matches(info)) {
                return section.toRule();
            }
        }

        return null;
    }

    public static DropFilter parse(Reader reader, String defaultName) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        DropFilter filter = new DropFilter(defaultName);
        FilterSection currentSection = null;

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // セクションヘッダー: [sound_name] または [sound_name: volume]
            if (line.startsWith("[") && line.endsWith("]")) {
                String content = line.substring(1, line.length() - 1).trim();
                String sound = content;
                float volume = 1.0f;

                int colonIdx = content.indexOf(':');
                if (colonIdx != -1) {
                    sound = content.substring(0, colonIdx).trim();
                    try {
                        volume = Float.parseFloat(content.substring(colonIdx + 1).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }

                currentSection = new FilterSection(sound, volume);
                filter.sections.add(currentSection);
            } else if (currentSection != null) {
                currentSection.addTarget(line);
            }
        }

        return filter;
    }
}
