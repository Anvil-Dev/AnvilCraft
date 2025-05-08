package dev.dubhe.anvilcraft.api.event;


import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityEvent;

@Getter
public class FallingBlockCollisionEvent extends EntityEvent {
    private final FallingBlockEntity fallingBlockEntity;
    private final BlockPos pos;
    private final Level level;
    private final double speed;

    /**
     * 铁砧撞击方块(仅侧向撞击)
     *
     * @param entity 撞击实体
     * @param pos    被撞击方块位置
     * @param level  世界
     * @param speed  撞击速度
     */
    public FallingBlockCollisionEvent(
        FallingBlockEntity entity,
        BlockPos pos,
        Level level,
        double speed
    ) {
        super(entity);
        this.fallingBlockEntity = entity;
        this.pos = pos;
        this.level = level;
        this.speed = speed;
    }
}
