package dev.dubhe.anvilcraft.block.entity.heatable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedhotBlockEntity extends HeatableBlockEntity {
    public RedhotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static RedhotBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new RedhotBlockEntity(type, pos, blockState);
    }
}
