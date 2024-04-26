package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.LightningStrikeEvent;
import dev.dubhe.anvilcraft.api.event.fabric.LightningBoltEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

public class LightningEventListener {
    /**
     * 初始化
     */
    public static void init() {
        LightningBoltEvent.LIGHTNING_STRIKE.register(LightningEventListener::onLightningStrike);
    }

    public static void onLightningStrike(Level level, BlockPos pos, LightningBolt entity) {
        AnvilCraft.EVENT_BUS.post(new LightningStrikeEvent(entity, pos, level));
    }
}
