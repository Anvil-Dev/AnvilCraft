package dev.dubhe.anvilcraft.event;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

@Getter
public class AnvilFallOnLandEvent {
    private final Level level;
    private final BlockPos pos;
    private final FallingBlockEntity entity;
    @Setter
    private boolean isAnvilDamage;

    public AnvilFallOnLandEvent(Level level, BlockPos pos, FallingBlockEntity entity) {
        this.level = level;
        this.pos = pos;
        this.entity = entity;
        this.isAnvilDamage = false;
    }
}
