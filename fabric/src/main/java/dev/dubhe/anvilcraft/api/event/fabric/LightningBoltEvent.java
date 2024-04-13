package dev.dubhe.anvilcraft.api.event.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * 闪电事件
 */
public class LightningBoltEvent {
    public static final Event<LightningStrike> LIGHTNING_STRIKE = EventFactory.createArrayBacked(LightningStrike.class,
        callbacks -> ((level, pos, entity) -> {
            for (LightningStrike callback : callbacks) {
                callback.strike(level, pos, entity);
            }
        }));

    /**
     * 雷击
     */
    @FunctionalInterface
    public interface LightningStrike {
        void strike(Level level, BlockPos pos, LightningBolt entity);
    }
}
