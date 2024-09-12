package dev.dubhe.anvilcraft.event.giantanvil;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiConsumer;

interface ShockBehaviorDefinition {
    boolean cornerMatches(BlockPos centerPos, Level level);

    default int priority() {
        return 1000;
    }

    void acceptRanges(List<BlockPos> blockPosList, Level level);

    // spotless:off
    abstract class Simple<T> implements ShockBehaviorDefinition {
        private final int[] dt = {-1, 1};
        final BiConsumer<List<BlockPos>, Level> rangeAcceptor;
        final T cornerBlock;

        public Simple(T cornerBlock, BiConsumer<List<BlockPos>, Level> rangeAcceptor) {
            this.rangeAcceptor = rangeAcceptor;
            this.cornerBlock = cornerBlock;
        }

        @Override
        public boolean cornerMatches(BlockPos centerPos, Level level) {
            for (int dx : dt) {
                for (int dz : dt) {
                    BlockPos pos =
                            new BlockPos(dx + centerPos.getX(), centerPos.getY(), dz + centerPos.getZ());
                    BlockState state = level.getBlockState(pos);
                    if (!blockMatches(state)) return false;
                }
            }
            return true;
        }

        protected abstract boolean blockMatches(BlockState state);

        @Override
        public void acceptRanges(List<BlockPos> blockPosList, Level level) {
            rangeAcceptor.accept(blockPosList, level);
        }
    }
    // spotless:on

    class SimpleTag extends Simple<TagKey<Block>> {

        public SimpleTag(TagKey<Block> cornerBlock, BiConsumer<List<BlockPos>, Level> rangeAcceptor) {
            super(cornerBlock, rangeAcceptor);
        }

        @Override
        protected boolean blockMatches(BlockState state) {
            return state.is(cornerBlock);
        }
    }

    class SimpleBlock extends Simple<Block> {

        public SimpleBlock(Block cornerBlock, BiConsumer<List<BlockPos>, Level> rangeAcceptor) {
            super(cornerBlock, rangeAcceptor);
        }

        @Override
        protected boolean blockMatches(BlockState state) {
            return state.is(cornerBlock);
        }
    }

    class MatchAll implements ShockBehaviorDefinition {

        final BiConsumer<List<BlockPos>, Level> rangeAcceptor;

        public MatchAll(BiConsumer<List<BlockPos>, Level> rangeAcceptor1) {
            this.rangeAcceptor = rangeAcceptor1;
        }

        @Override
        public int priority() {
            return 900;
        }

        @Override
        public void acceptRanges(List<BlockPos> blockPosList, Level level) {
            rangeAcceptor.accept(blockPosList, level);
        }

        @Override
        public boolean cornerMatches(BlockPos centerPos, Level level) {
            return true;
        }
    }
}
