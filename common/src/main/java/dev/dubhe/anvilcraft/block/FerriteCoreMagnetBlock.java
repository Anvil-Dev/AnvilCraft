package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FerriteCoreMagnetBlock extends MagnetBlock {
    public FerriteCoreMagnetBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, @NotNull RandomSource randomSource) {
        if (randomSource.nextInt(7) == 0) {
            serverLevel.setBlockAndUpdate(blockPos, ModBlocks.MAGNET_BLOCK.get().defaultBlockState());
        }
    }
}
