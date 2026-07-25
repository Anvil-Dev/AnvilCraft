package dev.dubhe.anvilcraft.api.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;

/// 可以像炼药锅一样参与流体配方的实体
public interface IEntityCauldron extends IFluidResourceHandlerHolder {
    default boolean anvilcraft$isIgnited() {
        return false;
    }

    default void anvilcraft$setIgnited(boolean ignited) {
    }
}
