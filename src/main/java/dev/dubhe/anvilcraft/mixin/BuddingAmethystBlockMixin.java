package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.block.IBuddingAmethystBlockExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Mixin(BuddingAmethystBlock.class)
public class BuddingAmethystBlockMixin implements IBuddingAmethystBlockExtension {
    @Shadow
    @Final
    private static Direction[] DIRECTIONS;

    @Override
    public void anvilcraft$tryGrowBuds(Level level, BlockPos pos, BlockState state) {
        List<Direction> budDirs = new ArrayList<>();
        for (Direction dir : BuddingAmethystBlockMixin.DIRECTIONS) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (
                neighborState.getBlock() instanceof AmethystClusterBlock
                && neighborState.getValue(AmethystClusterBlock.FACING) == dir
                && !neighborState.is(Blocks.AMETHYST_CLUSTER)
            ) {
                budDirs.add(dir);
            }
        }
        if (budDirs.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        Direction chosen = budDirs.get(random.nextInt(budDirs.size()));
        BlockPos budPos = pos.relative(chosen);
        BlockState budState = level.getBlockState(budPos);

        Block advancedBud = null;
        if (budState.is(Blocks.SMALL_AMETHYST_BUD)) {
            advancedBud = Blocks.MEDIUM_AMETHYST_BUD;
        } else if (budState.is(Blocks.MEDIUM_AMETHYST_BUD)) {
            advancedBud = Blocks.LARGE_AMETHYST_BUD;
        } else if (budState.is(Blocks.LARGE_AMETHYST_BUD)) {
            advancedBud = Blocks.AMETHYST_CLUSTER;
        }
        if (advancedBud == null) {
            return;
        }
        level.setBlockAndUpdate(
            budPos,
            advancedBud.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, chosen)
                .setValue(
                    AmethystClusterBlock.WATERLOGGED,
                    budState.getValue(AmethystClusterBlock.WATERLOGGED)
                )
        );
    }

    @Override
    public void anvilcraft$tryBreakClusters(Level level, BlockPos pos, BlockState state, BiConsumer<BlockPos, BlockState> breaker) {
        for (Direction dir : BuddingAmethystBlockMixin.DIRECTIONS) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (
                neighborState.getBlock() instanceof AmethystClusterBlock
                && neighborState.getValue(AmethystClusterBlock.FACING) == dir
                && neighborState.is(Blocks.AMETHYST_CLUSTER)
            ) {
                breaker.accept(neighborPos, neighborState);
            }
        }
    }
}
