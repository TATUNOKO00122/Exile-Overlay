package com.example.exile_overlay.dmgtracker.util;

import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillIdResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/SkillIdResolver");

    public static String resolveSkillId(DamageEvent event) {
        String ailmentId = event.data.getString(EventData.AILMENT);
        if (!ailmentId.isEmpty()) {
            boolean isProc = event.data.getBoolean("is_ailment_proc");
            return "ailment:" + ailmentId + (isProc ? "_proc" : "");
        }
        if (event.isSpell()) {
            return "spell:" + event.data.getString(EventData.SPELL);
        }
        if (event.data.isBasicAttack()) {
            var wt = event.data.getWeaponType();
            return "basic:" + (wt != null ? wt.id : "attack");
        }
        if (event.data.getBoolean(EventData.IS_SUMMON_ATTACK)) {
            return "summon:attack";
        }
        if (event.source instanceof net.minecraft.world.entity.OwnableEntity) {
            return "minion:attack";
        }
        LOGGER.debug("Unknown skill ID for DamageEvent. source: {}, isSpell: {}, isBasicAttack: {}", event.source, event.isSpell(), event.data.isBasicAttack());
        return "unknown";
    }

    private static java.lang.reflect.Method ailmentProcNameMethod = null;
    private static boolean ailmentProcMethodSearched = false;

    public static String resolveDisplayName(DamageEvent event) {
        String ailmentId = event.data.getString(EventData.AILMENT);
        if (!ailmentId.isEmpty()) {
            try {
                var ailment = ExileDB.Ailments().get(ailmentId);
                if (ailment != null) {
                    boolean isProc = event.data.getBoolean("is_ailment_proc");
                    if (isProc) {
                        if (!ailmentProcMethodSearched) {
                            ailmentProcMethodSearched = true;
                            try {
                                ailmentProcNameMethod = ailment.getClass().getMethod("procNameWord");
                            } catch (Exception ignored) { }
                        }
                        if (ailmentProcNameMethod != null) {
                            Object wordsEnum = ailmentProcNameMethod.invoke(ailment);
                            if (wordsEnum instanceof com.robertx22.mine_and_slash.uncommon.localization.Words) {
                                return ((com.robertx22.mine_and_slash.uncommon.localization.Words) wordsEnum).locNameLangFileGUID();
                            }
                        }
                    }
                    return ailment.locNameLangFileGUID();
                }
            } catch (Exception ignored) {
            }
            return "exile_overlay.tracker.ailment";
        }
        if (event.isSpell()) {
            Spell spell = event.getSpell();
            if (spell != null) {
                return spell.locNameLangFileGUID();
            }
            return "exile_overlay.tracker.unknown_spell";
        }
        if (event.data.isBasicAttack() || event.data.getBoolean(EventData.IS_SUMMON_ATTACK) || event.source instanceof net.minecraft.world.entity.OwnableEntity) {
            return "exile_overlay.tracker.basic_attack";
        }
        return "exile_overlay.tracker.unknown";
    }

    public static String resolveRawSpellId(DamageEvent event) {
        if (event.isSpell()) {
            return event.data.getString(EventData.SPELL);
        }
        return "";
    }

    public static String extractRawSpellId(String skillId) {
        if (skillId != null && skillId.startsWith("spell:")) {
            return skillId.substring(6);
        }
        return "";
    }

    public static boolean isBasicAttack(DamageEvent event) {
        return event.data.getBoolean(EventData.IS_BASIC_ATTACK);
    }

    public static boolean isAilment(DamageEvent event) {
        String ailmentId = event.data.getString(EventData.AILMENT);
        return ailmentId != null && !ailmentId.isEmpty();
    }

    public static boolean isSummonAttack(DamageEvent event) {
        return event.data.getBoolean(EventData.IS_SUMMON_ATTACK);
    }
}
