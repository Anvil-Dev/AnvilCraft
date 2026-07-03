package dev.dubhe.anvilcraft.block.entity.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 管道 BlockEntity 抽象基类。
 *
 * <p>重构后管道本体（直管/弯管/节点）<b>不再是方块实体</b>——它们是纯方块，
 * 由流体网络（{@link dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork}）
 * 从相邻容器 flood-fill 时顺带发现。本基类现仅供仍需 BlockEntity 的
 * {@link PumpBlockEntity}（耗电机器，需 tick 与状态同步）复用通用的方块更新工具。
 */
public abstract class AbstractPipeBlockEntity extends BlockEntity {
    protected AbstractPipeBlockEntity(BlockEntityType<? extends AbstractPipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 触发客户端渲染更新
     */
    protected void sendUpdate() {
        if (this.level == null) {
            return;
        }
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    /**
     * 触发邻居方块更新
     */
    protected void sendNeighbourUpdate() {
        if (this.level == null) {
            return;
        }
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
    }
}
