package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.ReinforcedConcreteHalf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

import static dev.dubhe.anvilcraft.block.state.ReinforcedConcreteHalf.BOTTOM;
import static dev.dubhe.anvilcraft.block.state.ReinforcedConcreteHalf.SINGLE;
import static dev.dubhe.anvilcraft.block.state.ReinforcedConcreteHalf.TOP;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReinforcedConcreteBlock extends Block {
    public static final EnumProperty<ReinforcedConcreteHalf> HALF =
        EnumProperty.create("half", ReinforcedConcreteHalf.class);

    /**
     * @param properties 方块属性
     */
    public ReinforcedConcreteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, SINGLE));
    }

    /**
     * When piston finished a block movement, this block will receive an NC update where neighborPos is
     * same as pos. So we cannot ignore the update when <code>neighborPos.equals(pos)</code>.
     *
     * @param pos     the position where block being updated
     * @param fromPos the position where block update is spread from
     * @return If the NC update should be ignored.
     * @see PistonMovingBlockEntity#tick(Level, BlockPos, BlockState, PistonMovingBlockEntity)
     */
    private static boolean shouldIgnoreUpdate(BlockPos pos, BlockPos fromPos) {
        return pos.getY() == fromPos.getY() && (pos.getX() != fromPos.getX() || pos.getZ() != fromPos.getZ());
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    private boolean checkHalf(BlockState state, ReinforcedConcreteHalf half) {
        return state.is(this) && state.getValue(HALF) == half;
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston) {
        if (level.isClientSide) return;
        if (shouldIgnoreUpdate(pos, neighborPos)) return;
        ReinforcedConcreteHalf half = state.getValue(HALF);
        BlockState aboveState = level.getBlockState(pos.above());
        BlockState belowState = level.getBlockState(pos.below());
        switch (half) {
            case TOP:
                if (this.checkHalf(belowState, SINGLE)) {
                    level.setBlock(pos.below(), state.setValue(HALF, BOTTOM), 2);
                } else if (!this.checkHalf(belowState, BOTTOM)) {
                    level.setBlock(pos, state.setValue(HALF, SINGLE), 2);
                }
                break;
            case BOTTOM:
                if (this.checkHalf(aboveState, SINGLE)) {
                    level.setBlock(pos.above(), state.setValue(HALF, TOP), 2);
                } else if (!this.checkHalf(aboveState, TOP)) {
                    level.setBlock(pos, state.setValue(HALF, SINGLE), 2);
                }
                break;
            case SINGLE:
                if (neighborPos.equals(pos.below()) && this.checkHalf(belowState, SINGLE)) {
                    level.setBlock(pos, state.setValue(HALF, TOP), 2);
                    level.setBlock(pos.below(), state.setValue(HALF, BOTTOM), 2);
                } else if (neighborPos.equals(pos.above()) && this.checkHalf(aboveState, SINGLE)) {
                    level.setBlock(pos, state.setValue(HALF, BOTTOM), 2);
                    level.setBlock(pos.above(), state.setValue(HALF, TOP), 2);
                }
        }
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        super.tick(state, level, pos, random);
    }
}
