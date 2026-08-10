package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 直管和弯管的 BlockEntity，负责 per-tick 重力排液逻辑。
 */
public class PipeBlockEntity extends AbstractPipeBlockEntity {

    protected PipeBlockEntity(BlockEntityType<PipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static PipeBlockEntity create(BlockEntityType<PipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeBlockEntity(type, pos, blockState);
    }

    public static int getEndCount(BlockState blockState) {
        if (!(blockState.getBlock() instanceof PipeStraightBlock) && !(blockState.getBlock() instanceof PipeCornerBlock)) {
            return -1;
        }
        int count = 0;
        if (blockState.getValue(PipeStraightBlock.HAS_END_START)) count++;
        if (blockState.getValue(PipeStraightBlock.HAS_END_END)) count++;
        return count;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Per-tick 排液逻辑。
     */
    public static void tick(Level level, BlockPos pos, BlockState state) {
        int endCount = PipeBlockEntity.getEndCount(state);
        if (endCount <= 0) return;
        boolean isStraight = state.getBlock() instanceof PipeStraightBlock;
        if (endCount == 2) {
            if (isStraight) {
                Direction.Axis axis = state.getValue(PipeStraightBlock.AXIS);
                Direction posDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
                Direction negDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
                PipeBlockEntity.tickEndCount2(level, pos, posDir, negDir);
                PipeBlockEntity.tickEndCount2(level, pos, negDir, posDir);
            } else {
                PipeBlock.CornerEnded cornerEnded = state.getValue(PipeCornerBlock.CORNER_ENDED);
                Direction firstDir = cornerEnded.getFirstDirection();
                Direction secondDir = cornerEnded.getSecondDirection();
                PipeBlockEntity.tickEndCount2(level, pos, firstDir, secondDir);
                PipeBlockEntity.tickEndCount2(level, pos, secondDir, firstDir);
            }
            return;
        }
        Direction sourceDirection;
        boolean hasEndStart = state.getValue(PipeBlock.HAS_END_START);
        if (isStraight) {
            if (hasEndStart) {
                sourceDirection = PipeBlock.getDirectionFromAxis(
                    state.getValue(PipeStraightBlock.AXIS), Direction.AxisDirection.NEGATIVE);
            } else {
                sourceDirection = PipeBlock.getDirectionFromAxis(
                    state.getValue(PipeStraightBlock.AXIS), Direction.AxisDirection.POSITIVE);
            }
        } else {
            if (hasEndStart) {
                sourceDirection = state.getValue(PipeCornerBlock.CORNER_ENDED).getFirstDirection();
            } else {
                sourceDirection = state.getValue(PipeCornerBlock.CORNER_ENDED).getSecondDirection();
            }
        }
        PipeEnd pipeEnd = AbstractPipeBlockEntity.getPipeEnd(level, pos, sourceDirection);
        if (pipeEnd == null) return;
        AbstractPipeBlockEntity.moveFluidWithHeightCheck(
            level, pos, sourceDirection, pipeEnd.pos(), pipeEnd.direction(), pipeEnd.effectiveHeight());
    }

    private static void tickEndCount2(Level level, BlockPos pos, Direction posDir, Direction negDir) {
        BlockPos targetCurPos = pos;
        Direction targetCurDir = negDir;
        int effectiveHeight = 0;
        BlockPos sourceNeighbor = pos.relative(posDir);
        if (level.getBlockState(sourceNeighbor).getBlock() instanceof PumpBlock) return;
        BlockPos targetNeighbor = pos.relative(negDir);
        if (level.getBlockState(targetNeighbor).getBlock() instanceof PumpBlock) {
            PipeEnd pumpEnd = AbstractPipeBlockEntity.getPipeEnd(level, targetNeighbor, negDir.getOpposite());
            if (pumpEnd != null) {
                targetCurPos = pumpEnd.pos();
                targetCurDir = pumpEnd.direction();
                effectiveHeight = pumpEnd.effectiveHeight();
            }
        }
        AbstractPipeBlockEntity.moveFluidWithHeightCheck(level, pos, posDir, targetCurPos, targetCurDir, effectiveHeight);
    }
}
