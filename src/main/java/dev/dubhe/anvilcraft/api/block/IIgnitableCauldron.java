package dev.dubhe.anvilcraft.api.block;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;

public interface IIgnitableCauldron {
    default boolean isEmpty(BlockCache cache, BlockPos pos) {
        return false;
    }

    default boolean isIgnited(BlockCache cache, BlockPos pos) {
        return false;
    }

    default void setIgnited(BlockCache cache, BlockPos pos, boolean ignited) {
    }

    Fluid getFluid(BlockCache cache, BlockPos pos);

    default boolean consumeOnce(BlockCache cache, BlockPos pos) {
        return false;
    }
}
