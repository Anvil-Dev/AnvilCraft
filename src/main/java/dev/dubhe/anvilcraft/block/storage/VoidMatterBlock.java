package dev.dubhe.anvilcraft.block.storage;

import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class VoidMatterBlock extends Block {
    public static final int VOID_DECAY_THRESHOLD = 5;

    public VoidMatterBlock(Properties properties) {
        super(properties.randomTicks());
    }

    public static BlockState voidDecay(Level level, RandomSource random) {
        Iterable<Holder<Block>> tagOrEmpty = level.registryAccess().lookupOrThrow(Registries.BLOCK)
            .getTagOrEmpty(ModBlockTags.VOID_DECAY_PRODUCTS);
        int count = 0;
        Block randomBlock = null;
        for (Holder<Block> blockHolder : tagOrEmpty) {
            count++;
            if (random.nextInt(count) == 0) {
                randomBlock = blockHolder.value();
            }
        }
        if (randomBlock == null) return Blocks.AIR.defaultBlockState();
        return randomBlock.defaultBlockState();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        long neighborVoidMatterCount = 0L;
        for (Direction d : Direction.values()) {
            BlockState b = level.getBlockState(pos.relative(d));
            if (b.getBlock() instanceof VoidMatterBlock) {
                neighborVoidMatterCount++;
            }
        }
        if (neighborVoidMatterCount >= VoidMatterBlock.VOID_DECAY_THRESHOLD) {
            level.setBlockAndUpdate(pos, VoidMatterBlock.voidDecay(level, random));
        }
    }

}
