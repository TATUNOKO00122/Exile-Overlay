package com.example.exile_overlay.dmgtracker.util;

import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;

public interface IDamageEventAccessor {
    DamageEvent.DmgByElement exileOverlay$getDmgByElement();
    void exileOverlay$setDmgByElement(DamageEvent.DmgByElement info);
}
