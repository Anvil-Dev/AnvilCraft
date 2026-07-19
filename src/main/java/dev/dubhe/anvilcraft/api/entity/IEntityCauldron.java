package dev.dubhe.anvilcraft.api.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;

public interface IEntityCauldron extends IFluidHandlerHolder {
    default boolean anvilcraft$isIgnited() {
        return false;
    }

    default void anvilcraft$setIgnited(boolean ignited) {
    }
}
