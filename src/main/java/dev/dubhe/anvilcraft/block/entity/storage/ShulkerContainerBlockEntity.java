package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.saved.storage.StorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ShulkerContainerBlockEntity extends StorageBlockEntity { // TODO: 实现潜影集装箱功能
    public ShulkerContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, StorageType.SHULKER_CONTAINER);
    }
}
