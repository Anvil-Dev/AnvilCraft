package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class LevitationPowderBlock extends Block {
    public LevitationPowderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState above = level.getBlockState(pos.above());
        //noinspection deprecation
        if (above.getBlock() instanceof FallingBlock || above.isAir() || above.liquid()) {
            LevitatingBlockEntity.levitate(level, pos, state);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, this.getDelayAfterPlace());
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
        boolean movedByPiston
    ) {
        BlockState above = level.getBlockState(pos.above());
        //noinspection deprecation
        if (above.getBlock() instanceof FallingBlock || above.isAir() || above.liquid()) {
            level.scheduleTick(pos, this, this.getDelayAfterPlace());
        }
    }

    protected int getDelayAfterPlace() {
        return 2;
    }
}
