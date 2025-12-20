package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.block.IEmberBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

public class EmberMetalStairBlock extends StairBlock implements IEmberBlock {
    @Getter
    @Setter
    private BlockState checkBlockState;

    public EmberMetalStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(SlabBlock.WATERLOGGED);
    }

    @Override
    public void randomTick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        if (random.nextDouble() <= 0.1) {
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
            level.setBlock(pos, state.setValue(SlabBlock.WATERLOGGED, false), 2);
        }
    }
}
