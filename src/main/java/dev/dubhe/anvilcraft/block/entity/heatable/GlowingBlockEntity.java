package dev.dubhe.anvilcraft.block.entity.heatable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GlowingBlockEntity extends HeatableBlockEntity {
    public GlowingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static GlowingBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new GlowingBlockEntity(type, pos, blockState);
    }
}
