package dev.dubhe.anvilcraft.block.entity.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPipeBlockEntity extends BlockEntity {
    protected AbstractPipeBlockEntity(
        BlockEntityType<? extends AbstractPipeBlockEntity> type,
        BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    protected void sendUpdate() {
        if (this.level == null) return;
        this.level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    protected void sendNeighbourUpdate() {
        if (this.level == null) return;
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
    }
}
