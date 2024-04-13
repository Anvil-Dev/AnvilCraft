package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import dev.dubhe.anvilcraft.api.event.fabric.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

public class AnvilEntityEvent {
    /**
     * 初始化
     */
    public static void init() {
        AnvilEvent.ON_LAND.register(AnvilEntityEvent::onLand);
        AnvilEvent.HURT_ENTITY.register(AnvilEntityEvent::hurt);
    }

    private static boolean onLand(Level level, BlockPos pos, FallingBlockEntity entity, float fallDistance) {
        AnvilFallOnLandEvent event = new AnvilFallOnLandEvent(level, pos, entity, fallDistance);
        AnvilCraft.EVENT_BUS.post(event);
        return event.isAnvilDamage();
    }

    private static void hurt(FallingBlockEntity entity, BlockPos pos, Level level, Entity hurtedEntity, float damage) {
        AnvilCraft.EVENT_BUS.post(new AnvilHurtEntityEvent(entity, pos, level, hurtedEntity, damage));
    }
}
