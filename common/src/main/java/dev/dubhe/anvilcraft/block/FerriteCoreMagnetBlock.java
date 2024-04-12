package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    public void randomTick(
        @NotNull BlockState blockState,
        @NotNull ServerLevel serverLevel,
        @NotNull BlockPos blockPos,
        @NotNull RandomSource randomSource
    ) {
        int times = 0;
        for (Direction face : Direction.values()) {
            if (serverLevel.getBlockState(blockPos.relative(face)).is(ModBlocks.MAGNET_BLOCK.get())) times++;
        }
        if (randomSource.nextInt(7) <= times) {
            serverLevel.setBlockAndUpdate(blockPos, ModBlocks.MAGNET_BLOCK.get().defaultBlockState());
        }
    }
}
