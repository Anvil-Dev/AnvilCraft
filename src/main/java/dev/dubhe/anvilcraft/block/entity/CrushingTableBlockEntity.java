package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 粉碎台方块实体，用于存储原料与产物，并在铁坷砸落时执行粉碎配方。
 */
public class CrushingTableBlockEntity extends ProcessingTableBlockEntity {
    public CrushingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
