package dev.dubhe.anvilcraft.block;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class EmberMetalSlabBlock extends SlabBlock implements EmberBlock {
    @Getter
    @Setter
    private BlockState checkBlockState;

    public EmberMetalSlabBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(@NotNull BlockState state) {
        return state.getValue(SlabBlock.WATERLOGGED);
    }

    @Override
    public void randomTick(
            @NotNull BlockState state,
            @NotNull ServerLevel level,
            @NotNull BlockPos pos,
            @NotNull RandomSource random
    ) {
        if (random.nextDouble() <= 0.1) {
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
            level.setBlock(pos, state.setValue(SlabBlock.WATERLOGGED, false), 2);
        }
    }
}
