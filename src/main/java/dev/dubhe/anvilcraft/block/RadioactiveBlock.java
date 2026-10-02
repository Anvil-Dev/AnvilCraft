package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 放射性储存块（铀块、钚块）。
 *
 * <p>钚块在随机刻检测六向相邻方块，只要任意一面接触水或含水方块就不会融毁；
 * 六面均未接触水或含水方块时融毁为岩浆块。铀块不会融毁。</p>
 */
public class RadioactiveBlock extends Block {
    private final boolean meltdownEnabled;

    public RadioactiveBlock(Properties properties, boolean meltdownEnabled) {
        super(meltdownEnabled ? properties.randomTicks() : properties);
        this.meltdownEnabled = meltdownEnabled;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.meltdownEnabled) return;

        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.getFluidState().is(FluidTags.WATER)) return;
        }

        level.setBlockAndUpdate(pos, Blocks.MAGMA_BLOCK.defaultBlockState());
    }
}
