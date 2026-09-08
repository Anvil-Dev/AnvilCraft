package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LargeCrateBlockEntity extends StorageBlockEntity {
    public LargeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, ModStorageTypes.LARGE_CRATE);
    }

    @Override
    public void refreshComparatorSignal() {
        Level level = this.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof LargeCrateBlock block)) {
            super.refreshComparatorSignal();
            return;
        }
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos partPos = block.getMainPartPos(this.getBlockPos(), state).offset(part.getOffset());
            if (level.getBlockState(partPos).getBlock() instanceof LargeCrateBlock) {
                level.updateNeighbourForOutputSignal(partPos, block);
            }
        }
    }
}
