package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.ReinforcedConcreteHalf;
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
    public static final EnumProperty<ReinforcedConcreteHalf> HALF = EnumProperty
        .create("half", ReinforcedConcreteHalf.class);

    /**
     * @param properties 方块属性
     */
    public ReinforcedConcreteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HALF, ReinforcedConcreteHalf.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(
        @NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    
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
        ReinforcedConcreteHalf half = state.getValue(HALF);
        if (half != ReinforcedConcreteHalf.SINGLE) {
            if (half == ReinforcedConcreteHalf.TOP && !level.getBlockState(pos.below()).is(this)) {
                level.setBlock(pos, state.setValue(HALF, ReinforcedConcreteHalf.SINGLE), 2);
                return;
            }
            if (half == ReinforcedConcreteHalf.BOTTOM && !level.getBlockState(pos.above()).is(this)) {
                level.setBlock(pos, state.setValue(HALF, ReinforcedConcreteHalf.SINGLE), 2);
            }
        } else {
            if (level.getBlockState(pos.above()).is(this)
                && level.getBlockState(pos.above()).getValue(HALF) == ReinforcedConcreteHalf.SINGLE) {
                level.setBlock(pos, state.setValue(HALF, ReinforcedConcreteHalf.BOTTOM), 2);
                level.setBlock(pos.above(), state.setValue(HALF, ReinforcedConcreteHalf.TOP), 2);
                return;
            }
            if (level.getBlockState(pos.below()).is(this)
                && level.getBlockState(pos.below()).getValue(HALF) == ReinforcedConcreteHalf.SINGLE) {
                level.setBlock(pos, state.setValue(HALF, ReinforcedConcreteHalf.TOP), 2);
                level.setBlock(pos.below(), state.setValue(HALF, ReinforcedConcreteHalf.BOTTOM), 2);
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
