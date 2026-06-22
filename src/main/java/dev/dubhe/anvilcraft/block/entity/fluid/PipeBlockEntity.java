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

/**
 * 直管和弯管的 BlockEntity，负责 per-tick 重力排液逻辑。
 *
 * <h3>排液规则</h3>
 * <ul>
 *   <li>端头数 ≤ 0 → 无排液（无端点）</li>
 *   <li>两端端头、水平直管 → 无排液（水平管不自动排液）</li>
 *   <li>两端端头、水平弯管 → 无排液</li>
 *   <li>两端端头、垂直直管 → 上方端排向下方端（同柱排液）</li>
 *   <li>两端端头、垂直弯管 → 垂直端排向水平端（或反向）</li>
 *   <li>一端端头 → 沿无端头方向追踪 PipeEnd，向该终点排液</li>
 * </ul>
 */
public class PipeBlockEntity extends AbstractPipeBlockEntity {

    protected PipeBlockEntity(BlockEntityType<PipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static PipeBlockEntity create(BlockEntityType<PipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeBlockEntity(type, pos, blockState);
    }

    /**
     * 统计直管/弯管的端头数量（HAS_END_START + HAS_END_END 为 true 的个数）。
     *
     * @return 端头数，非直管/弯管返回 -1
     */
    public static int getEndCount(BlockState blockState) {
        if (!(blockState.getBlock() instanceof PipeStraightBlock) && !(blockState.getBlock() instanceof PipeCornerBlock)) {
            return -1;
        }
        int count = 0;
        if (blockState.getValue(PipeStraightBlock.HAS_END_START)) {
            count++;
        }
        if (blockState.getValue(PipeStraightBlock.HAS_END_END)) {
            count++;
        }
        return count;
    }

    // ---- NBT 持久化 heightBonus ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- Per-tick 排液 ----

    /**
     * Per-tick 排液逻辑。
     *
     * <p>仅当有端头时才执行。两端端头时按管轴方向排液，
     * 端头指向泵时自动透传追踪（不提前返回）。
     * 单端端头时沿无端头方向追踪 PipeEnd 并向其排液。
     */
    public static void tick(Level level, BlockPos pos, BlockState state) {
        int endCount = getEndCount(state);
        if (endCount <= 0) {
            return;
        }

        boolean isStraight = state.getBlock() instanceof PipeStraightBlock;

        if (endCount == 2) {
            // 两端端头 → 按管轴方向排液，检查端头是否指向泵
            if (isStraight) {
                Direction.Axis axis = state.getValue(PipeStraightBlock.AXIS);
                Direction posDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
                Direction negDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);

                // 检查端头指向的方块是否是泵，若是则透传追踪
                tickEndCount2(level, pos, posDir, negDir);
                tickEndCount2(level, pos, negDir, posDir);
            } else {
                // 弯管两端端头
                PipeBlock.CornerEnded cornerEnded = state.getValue(PipeCornerBlock.CORNER_ENDED);
                Direction firstDir = cornerEnded.getFirstDirection();
                Direction secondDir = cornerEnded.getSecondDirection();

                tickEndCount2(level, pos, firstDir, secondDir);
                tickEndCount2(level, pos, secondDir, firstDir);
            }
            return;
        }

        // 单端端头 → 沿无端头方向追踪 PipeEnd，向终点排液
        Direction sourceDirection;
        boolean hasEndStart = state.getValue(PipeBlock.HAS_END_START);
        if (isStraight) {
            // 直管：端头端是源，无端头端追踪出口
            if (hasEndStart) {
                sourceDirection = PipeBlock.getDirectionFromAxis(state.getValue(PipeStraightBlock.AXIS), Direction.AxisDirection.NEGATIVE);
            } else {
                sourceDirection = PipeBlock.getDirectionFromAxis(state.getValue(PipeStraightBlock.AXIS), Direction.AxisDirection.POSITIVE);
            }
        } else {
            // 弯管：端头端是源，无端头端追踪出口
            if (hasEndStart) {
                sourceDirection = state.getValue(PipeCornerBlock.CORNER_ENDED).getFirstDirection();
            } else {
                sourceDirection = state.getValue(PipeCornerBlock.CORNER_ENDED).getSecondDirection();
            }
        }
        PipeEnd pipeEnd = PipeBlockEntity.getPipeEnd(level, pos, sourceDirection);
        if (pipeEnd == null) {
            return;
        }
        AbstractPipeBlockEntity.moveFluidWithHeightCheck(
            level,
            pos,
            sourceDirection,
            pipeEnd.pos(),
            pipeEnd.direction(),
            pipeEnd.effectiveHeight()
        );
    }

    private static void tickEndCount2(Level level, BlockPos pos, Direction posDir, Direction negDir) {
        BlockPos targetCurPos = pos;
        Direction targetCurDir = negDir;
        int effectiveHeight = 0;

        BlockPos sourceNeighbor = pos.relative(posDir);
        if (level.getBlockState(sourceNeighbor).getBlock() instanceof PumpBlock) {
            return;
        }

        BlockPos targetNeighbor = pos.relative(negDir);
        if (level.getBlockState(targetNeighbor).getBlock() instanceof PumpBlock) {
            PipeEnd pumpEnd = getPipeEnd(level, targetNeighbor, negDir.getOpposite());
            if (pumpEnd != null) {
                targetCurPos = pumpEnd.pos();
                targetCurDir = pumpEnd.direction();
                effectiveHeight = pumpEnd.effectiveHeight();
            }
        }

        AbstractPipeBlockEntity.moveFluidWithHeightCheck(
            level,
            pos,
            posDir,
            targetCurPos,
            targetCurDir,
            effectiveHeight
        );
    }
}
