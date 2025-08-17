package dev.dubhe.anvilcraft.block.entity.heatable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class OverheatedBlockEntity extends HeatableBlockEntity {
    public OverheatedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static OverheatedBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new OverheatedBlockEntity(type, pos, blockState);
    }
}
