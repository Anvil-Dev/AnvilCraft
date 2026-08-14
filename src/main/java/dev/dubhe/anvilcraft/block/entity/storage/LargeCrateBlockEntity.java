package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.saved.storage.StorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LargeCrateBlockEntity extends StorageBlockEntity {
    public LargeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, StorageType.LARGE_CRATE);
    }
}
