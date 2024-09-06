package dev.dubhe.anvilcraft.api.event.forge;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityEvent;

@Getter
public class LightningBoltStrikeEvent extends EntityEvent {
    private final Level level;
    private final BlockPos pos;
    private final LightningBolt entity;

    /**
     * 雷击事件
     *
     * @param entity 闪电
     * @param level  世界
     * @param pos    位置
     */
    public LightningBoltStrikeEvent(LightningBolt entity, Level level, BlockPos pos) {
        super(entity);
        this.level = level;
        this.pos = pos;
        this.entity = entity;
    }
}
