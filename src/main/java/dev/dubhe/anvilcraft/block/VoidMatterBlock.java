package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class VoidMatterBlock extends Block {
    public static final int VOID_DECAY_THRESHOLD = 5;

    public VoidMatterBlock(Properties properties) {
        super(properties.randomTicks());
    }

    public static BlockState voidDecay(Level level, BlockPos pos, BlockState state, RandomSource random) {
        return level.registryAccess().registryOrThrow(Registries.BLOCK)
            .getTag(ModBlockTags.VOID_DECAY_PRODUCTS)
            .flatMap(it -> it.getRandomElement(random))
            .map(h -> h.value().defaultBlockState())
            .orElse(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        long neighborVoidMatterCount = Direction.stream()
            .map(d -> level.getBlockState(pos.relative(d)))
            .filter(b -> b.getBlock() instanceof VoidMatterBlock)
            .count();
        if (neighborVoidMatterCount >= VOID_DECAY_THRESHOLD) {
            level.setBlockAndUpdate(pos, voidDecay(level, pos, state, random));
        }
    }

}
