package dev.dubhe.anvilcraft.block.entity.heatable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class IncandescentBlockEntity extends HeatableBlockEntity {
    public IncandescentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static IncandescentBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new IncandescentBlockEntity(type, pos, blockState);
    }
}
