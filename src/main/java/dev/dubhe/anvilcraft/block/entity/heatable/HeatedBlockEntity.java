package dev.dubhe.anvilcraft.block.entity.heatable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class HeatedBlockEntity extends HeatableBlockEntity {
    public HeatedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static HeatedBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new HeatedBlockEntity(type, pos, blockState);
    }
}
