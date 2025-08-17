package dev.dubhe.anvilcraft.api.heat;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriPredicate;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class HeatableBlockEntry implements Comparable<HeatableBlockEntry> {
    private final HeatTier tier;
    private final Block defaultBlock;

    protected HeatableBlockEntry(HeatTier tier, Block defaultBlock) {
        this.tier = tier;
        this.defaultBlock = defaultBlock;
    }

    public abstract boolean isValidBlock(Level level, BlockPos pos, BlockState state);

    @Override
    public int compareTo(@NotNull HeatableBlockEntry o) {
        return this.tier.compareTo(o.tier);
    }

    public static HeatableBlockEntry predicate(HeatTier tier, Block defaultBlock, TriPredicate<Level, BlockPos, BlockState> predicate) {
        return new Predicate(tier, defaultBlock, predicate);
    }

    public static HeatableBlockEntry simple(HeatTier tier, Block defaultBlock) {
        return new Simple(tier, defaultBlock);
    }

    static class Predicate extends HeatableBlockEntry {
        private final TriPredicate<Level, BlockPos, BlockState> predicate;

        protected Predicate(HeatTier tier, Block defaultBlock, TriPredicate<Level, BlockPos, BlockState> predicate) {
            super(tier, defaultBlock);
            this.predicate = predicate;
        }

        @Override
        public boolean isValidBlock(Level level, BlockPos pos, BlockState state) {
            return this.predicate.test(level, pos, state);
        }
    }

    static class Simple extends HeatableBlockEntry {
        protected Simple(HeatTier tier, Block defaultBlock) {
            super(tier, defaultBlock);
        }

        @Override
        public boolean isValidBlock(Level level, BlockPos pos, BlockState state) {
            return state.is(this.getDefaultBlock());
        }
    }
}
