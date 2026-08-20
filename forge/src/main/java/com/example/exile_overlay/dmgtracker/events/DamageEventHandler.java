package com.example.exile_overlay.dmgtracker.events;

import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DamageEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageEventHandler");

    public static void register() {
        // [設計思想]
        // 以前は ExileEvents.DAMAGE_AFTER_CALC を利用していましたが、
        // M&Sの仕様により特定の条件（死亡時など）でしか発火しないことが判明したため廃止しました。
        // 現在は DamageEventMixin.java において、DamageEvent.activate() の完了時に直接トラッキングを行っています。
    }
}
