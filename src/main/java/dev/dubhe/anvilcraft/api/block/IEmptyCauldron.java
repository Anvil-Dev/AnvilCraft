package dev.dubhe.anvilcraft.api.block;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public interface IEmptyCauldron extends IIgnitableCauldron {
    @Override
    default boolean isEmpty(BlockCache cache, BlockPos pos) {
        return true;
    }

    @Override
    default boolean isIgnited(BlockCache cache, BlockPos pos) {
        return true;
    }

    @Override
    default Fluid getFluid(BlockCache cache, BlockPos pos) {
        return Fluids.EMPTY;
    }
}
