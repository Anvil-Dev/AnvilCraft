package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CelestialForgingAnvilBlockEntity extends BlockEntity {
    @Getter
    private int rotation = 0;

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void clientTick() {
        if (this.rotation == 360) {
            this.rotation = 0;
        }
        this.rotation += 3;
    }
}
