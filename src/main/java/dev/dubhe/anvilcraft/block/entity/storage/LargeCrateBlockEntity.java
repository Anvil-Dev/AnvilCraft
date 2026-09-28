package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LargeCrateBlockEntity extends StorageBlockEntity {
    public LargeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, StorageType.LARGE_CRATE);
    }

    @Override
    public void refreshComparatorSignal() {
        if (this.level == null || this.level.isClientSide()) return;
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof LargeCrateBlock block)) {
            super.refreshComparatorSignal();
            return;
        }
        BlockPos mainPos = block.getMainPartPos(this.getBlockPos(), state);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos pos = mainPos.offset(part.getOffset());
            if (this.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && this.level.getBlockState(pos).is(block)) {
                this.level.updateNeighbourForOutputSignal(pos, block);
            }
        }
    }
}
