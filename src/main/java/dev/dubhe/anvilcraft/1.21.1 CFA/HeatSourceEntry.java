package dev.dubhe.anvilcraft.api.heat.collector;

import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

@Getter
public abstract class HeatSourceEntry {
    private final int charge;

    public HeatSourceEntry(int charge) {
        this.charge = charge;
    }

    public abstract int accepts(BlockState state);

    public abstract BlockState transform(BlockState state);

    public int timeToTransform() {
        return 40;
    }

    public static HeatSourceEntry predicate(
        int charge,
        java.util.function.Predicate<BlockState> predicate,
        Function<BlockState, BlockState> transformer
    ) {
        return new Predicate(charge, predicate, transformer);
    }

    public static HeatSourceEntry predicateAlways(
        int charge,
        java.util.function.Predicate<BlockState> predicate
    ) {
        return new Predicate(charge, predicate, Function.identity());
    }

    public static HeatSourceEntry simple(int charge, Block input, Block output) {
        return new Simple(charge, input, output);
    }

    /**
     * not really forever
     */
    public static HeatSourceEntry forever(int charge, Block block) {
        return new Always(charge, block);
    }

    static class Predicate extends HeatSourceEntry {
        private final java.util.function.Predicate<BlockState> input;
        private final Function<BlockState, BlockState> transformer;

        public Predicate(
            int charge,
            java.util.function.Predicate<BlockState> input,
            Function<BlockState, BlockState> transformer
        ) {
            super(charge);
            this.input = input;
            this.transformer = transformer;
        }

        @Override
        public int accepts(BlockState state) {
            if (input.test(state)) {
                return this.getCharge();
            }
            return 0;
        }

        @Override
        public BlockState transform(BlockState state) {
            return transformer.apply(state);
        }
    }

    static class Simple extends HeatSourceEntry {
        private final Block input;
        private final Block output;

        public Simple(int charge, Block input, Block output) {
            super(charge);
            this.input = input;
            this.output = output;
        }

        @Override
        public int accepts(BlockState state) {
            return state.is(input) ? getCharge() : 0;
        }

        @Override
        public BlockState transform(BlockState state) {
            return output.defaultBlockState();
        }
    }

    static class Always extends HeatSourceEntry {
        private final Block input;

        public Always(int charge, Block input) {
            super(charge);
            this.input = input;
        }

        @Override
        public int accepts(BlockState state) {
            return state.is(input) ? getCharge() : 0;
        }

        @Override
        public BlockState transform(BlockState state) {
            return input.defaultBlockState();
        }

        @Override
        public int timeToTransform() {
            return Integer.MAX_VALUE;
        }
    }
}
