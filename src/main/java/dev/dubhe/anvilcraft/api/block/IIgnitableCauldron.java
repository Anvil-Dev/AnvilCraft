package dev.dubhe.anvilcraft.api.block;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;

import java.util.OptionalInt;

public interface IIgnitableCauldron extends ICauldron {
    default boolean isEmpty(BlockCache cache, BlockPos pos) {
        return false;
    }

    default boolean isIgnited(BlockCache cache, BlockPos pos) {
        return false;
    }

    default void setIgnited(BlockCache cache, BlockPos pos, boolean ignited) {
    }

    Fluid getFluid(BlockCache cache, BlockPos pos);

    default int getFluidAmount(BlockCache cache, BlockPos pos) {
        return Integer.MAX_VALUE;
    }

    default OptionalInt consumeOnce(BlockCache cache, BlockPos pos, boolean simulate) {
        return OptionalInt.empty();
    }
}
