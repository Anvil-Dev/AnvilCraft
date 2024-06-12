package dev.dubhe.anvilcraft.api.chargecollector;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;

import java.util.function.Function;

@Getter
public abstract class ThermoEntry {
    private final int charge;

    public ThermoEntry(int charge) {
        this.charge = charge;
    }

    public int ttl() {
        return 2;
    }

    public static ThermoEntry predicate(
            int charge,
            java.util.function.Predicate<BlockState> predicate,
            Function<BlockState, BlockState> transformer) {
        return new Predicate(charge, predicate, transformer);
    }

    public abstract int accepts(BlockState state);

    public abstract BlockState transform(BlockState state);

    public static ThermoEntry simple(int charge, Block input, Block output) {
        return new Simple(charge, input, output);
    }

    public static ThermoEntry always(int charge, Block block) {
        return new Always(charge, block);
    }

    static class Predicate extends ThermoEntry {
        private final java.util.function.Predicate<BlockState> input;
        private final Function<BlockState, BlockState> transformer;

        public Predicate(
                int charge,
                java.util.function.Predicate<BlockState> input,
                Function<BlockState, BlockState> transformer) {
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

    static class Simple extends ThermoEntry {

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

    static class Always extends ThermoEntry {

        private final Block block;

        public Always(int charge, Block input) {
            super(charge);
            this.block = input;
        }

        @Override
        public int accepts(BlockState state) {
            return state.is(block) ? getCharge() : 0;
        }

        @Override
        public BlockState transform(BlockState state) {
            return block.defaultBlockState();
        }

        @Override
        public int ttl() {
            return Integer.MAX_VALUE;
        }
    }
}
