package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 拆包台方块实体，用于存储原料，并在铁坷砸落时执行拆包配方。
 */
public class UnpackingTableBlockEntity extends ProcessingTableBlockEntity {
    public UnpackingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
