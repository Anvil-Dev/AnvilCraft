package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.Half;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;

public class ReinforcedConcreteBlock extends Block {
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);

    /**
     * @param properties 方块属性
     */
    public ReinforcedConcreteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HALF, Half.MID));
    }

    @Override
    protected void createBlockStateDefinition(
        @NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) return;
        if (neighborPos.getY() == pos.getY()) return;
        Half half = state.getValue(HALF);
        if (half != Half.MID) {
            if (half == Half.TOP && !level.getBlockState(pos.below()).is(this)) {
                level.setBlock(pos, state.setValue(HALF, Half.MID), 2);
                return;
            }
            if (half == Half.BOTTOM && !level.getBlockState(pos.above()).is(this)) {
                level.setBlock(pos, state.setValue(HALF, Half.MID), 2);
            }
        } else {
            if (level.getBlockState(pos.above()).is(this)
                && level.getBlockState(pos.above()).getValue(HALF) == Half.MID) {
                level.setBlock(pos, state.setValue(HALF, Half.BOTTOM), 2);
                level.setBlock(pos.above(), state.setValue(HALF, Half.TOP), 2);
                return;
            }
            if (level.getBlockState(pos.below()).is(this)
                && level.getBlockState(pos.below()).getValue(HALF) == Half.MID) {
                level.setBlock(pos, state.setValue(HALF, Half.TOP), 2);
                level.setBlock(pos.below(), state.setValue(HALF, Half.BOTTOM), 2);
            }
        }
    }

    @Override
    public void tick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random) {
        super.tick(state, level, pos, random);
    }
}
