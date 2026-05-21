package dev.dubhe.anvilcraft.api.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public interface INegativeShapeBlock<T> extends IHammerRemovable, Predicate<BlockState> {
    Class<T> getBlockType();

    @Override
    default boolean test(BlockState blockState) {
        return this.getBlockType().isInstance(blockState.getBlock());
    }
}
