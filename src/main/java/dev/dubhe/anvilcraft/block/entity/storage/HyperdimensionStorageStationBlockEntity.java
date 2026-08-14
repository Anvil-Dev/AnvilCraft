package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.saved.storage.StorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class HyperdimensionStorageStationBlockEntity extends StorageBlockEntity {
    public HyperdimensionStorageStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, StorageType.HYPERDIMENSION);
    }
}
