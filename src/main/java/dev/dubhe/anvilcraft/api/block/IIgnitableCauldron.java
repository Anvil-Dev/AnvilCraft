package dev.dubhe.anvilcraft.api.block;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.UnknownNullability;

public interface IIgnitableCauldron {
    default boolean isIgnited(BlockCache cache, BlockPos pos) {
        return true;
    }

    default void setIgnited(BlockCache cache, BlockPos pos, boolean ignited) {
    }

    Fluid getFluid(BlockCache cache, BlockPos pos);

    default boolean consumeOnce(BlockCache cache, BlockPos pos) {
        return false;
    }
}
