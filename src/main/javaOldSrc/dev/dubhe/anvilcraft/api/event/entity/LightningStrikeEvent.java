package dev.dubhe.anvilcraft.api.event.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

public class LightningStrikeEvent extends EntityEvent<LightningBolt> {
    public LightningStrikeEvent(LightningBolt entity, BlockPos pos, Level level) {
        super(entity, pos, level);
    }
}
