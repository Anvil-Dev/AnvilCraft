package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CrateBlockEntity extends StorageBlockEntity {
    public CrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, ModStorageTypes.CRATE);
    }
}
