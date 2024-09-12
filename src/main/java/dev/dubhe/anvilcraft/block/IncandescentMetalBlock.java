package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.NotNull;

public class IncandescentMetalBlock extends RedhotMetalBlock {
    public IncandescentMetalBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    public void onPlace(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState oldState,
            boolean movedByPiston) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            checkWater(level, neighbour);
        }
    }

    @Override
    public void neighborChanged(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Block neighborBlock,
            @NotNull BlockPos neighborPos,
            boolean movedByPiston) {
        checkWater(level, neighborPos);
    }

    private void checkWater(Level level, BlockPos neighborPos) {
        FluidState neighbourState = level.getFluidState(neighborPos);
        BlockState blockState = level.getBlockState(neighborPos);
        if (neighbourState.is(Fluids.WATER) || neighbourState.is(Fluids.FLOWING_WATER)) {
            if (blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                level.setBlock(
                        neighborPos, blockState.setValue(BlockStateProperties.WATERLOGGED, false), 3);
            } else {
                if (blockState.is(Blocks.WATER)) {
                    level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            level.playSound(
                    null,
                    neighborPos,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.5F,
                    2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);

            for (int l = 0; l < 8; ++l) {
                level.addParticle(
                        ParticleTypes.LARGE_SMOKE,
                        (double) neighborPos.getX() + Math.random(),
                        (double) neighborPos.getY() + Math.random(),
                        (double) neighborPos.getZ() + Math.random(),
                        0.0,
                        0.0,
                        0.0);
            }
        }
    }
}
