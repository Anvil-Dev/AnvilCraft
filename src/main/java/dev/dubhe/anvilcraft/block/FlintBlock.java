package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.CauldronIgnition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class FlintBlock extends Block {
    public FlintBlock(Properties properties) {
        super(properties);
    }

    public static void ignite(LevelAccessor level, BlockPos pos, boolean isFlint) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean flag = false;
        for (Direction direction : Direction.values()) {
            BlockState blockState = level.getBlockState(pos.relative(direction));
            if (isFlint) {
                if (blockState.is(Blocks.IRON_BLOCK) || blockState.is(ModBlocks.HEAVY_IRON_BLOCK)) {
                    flag = true;
                    break;
                }
            } else {
                if (blockState.is(ModBlocks.FLINT_BLOCK)) {
                    flag = true;
                    break;
                }
            }
        }
        if (flag) {
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    for (int z = -1; z < 2; z++) {
                        BlockPos offset = pos.offset(x, y, z);
                        BlockState blockState = level.getBlockState(offset);
                        if (FlintBlock.tryIgniteAt(serverLevel, offset, blockState)) {
                            FlintBlock.playIgniteSound(level, offset);
                            return;
                        }
                        blocks.add(offset);
                    }
                }
            }

            List<BlockPos> newBlocks = new ArrayList<>();
            for (BlockPos blockPos : blocks) {
                if (BaseFireBlock.canBePlacedAt(serverLevel, blockPos.above(), Direction.UP)) {
                    newBlocks.add(blockPos);
                }
            }
            if (!newBlocks.isEmpty()) {
                BlockPos blockPos = newBlocks.get(level.getRandom().nextIntBetweenInclusive(0, newBlocks.size() - 1));
                level.setBlock(blockPos.above(), BaseFireBlock.getState(level, blockPos), 3);
                FlintBlock.playIgniteSound(level, blockPos);
            }
        }
    }

    private static boolean tryIgniteAt(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FishTankBlock || state.getBlock() instanceof LargeCauldronBlock) {
            return CauldronIgnition.tryIgnite(level, pos, state);
        }
        if (state.getBlock() instanceof IIgnitableCauldron cauldron) {
            BlockCache cache = new BlockCache(level);
            if (!cauldron.isIgnited(cache, pos)) {
                cauldron.setIgnited(cache, pos, true);
                cache.accept();
                return true;
            }
        } else if (state.getBlock() instanceof CampfireBlock && !state.getValue(CampfireBlock.LIT)) {
            return level.setBlock(pos, state.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
        }
        return false;
    }

    private static void playIgniteSound(LevelAccessor level, BlockPos pos) {
        level.playSound(
            null,
            pos,
            SoundEvents.FLINTANDSTEEL_USE,
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.4F + 0.8F
        );
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (movedByPiston) {
            ignite(level, pos, true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (movedByPiston) {
            ignite(level, pos, true);
        }
    }
}
