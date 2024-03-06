package dev.dubhe.anvilcraft.event;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

@Setter
@Getter
public class AnvilFallOnLandEvent extends EntityEvent<FallingBlockEntity> {
    private boolean isAnvilDamage;

    public AnvilFallOnLandEvent(Level level, BlockPos pos, FallingBlockEntity entity) {
        super(entity, pos, level);
        this.isAnvilDamage = false;
    }
}
