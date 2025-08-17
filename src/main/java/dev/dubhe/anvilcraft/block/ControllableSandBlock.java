package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.entity.StandableFallingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableLevitatingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ControllableSandBlock extends Block implements IHammerRemovable {
    public ControllableSandBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("ConstantValue")
    protected void move(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.hasNeighborSignal(pos)) return;
        boolean aboveIsFree = StandableFallingBlockEntity.isFree(level, pos.above());
        boolean belowIsFree = StandableFallingBlockEntity.isFree(level, pos.below());
        if (!aboveIsFree && !belowIsFree) return;
        if (aboveIsFree && belowIsFree) {
            if (random.nextFloat() > 0.5f) aboveIsFree = false;
            else belowIsFree = false;
        }

        if (!aboveIsFree) {
            StandableFallingBlockEntity.fall(level, pos, state);
        } else if (!belowIsFree) {
            StandableLevitatingBlockEntity.levitate(level, pos, state);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.hasNeighborSignal(pos)) return;
        this.move(state, level, pos, level.getRandom());
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston
    ) {
        if (!level.hasNeighborSignal(pos)) return;
        this.move(state, level, pos, level.getRandom());
    }
}
