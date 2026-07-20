package dev.dubhe.anvilcraft.api.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;

public interface IEntityCauldron extends IFluidHandlerHolder {
    /**
     * 流体管网是否应按原版炼药锅的整容器规则转移此实体中的内容。
     * 使用普通储罐实现的实体炼药锅可覆写此方法，以允许任意数量的流体转移。
     */
    default boolean anvilcraft$usesWholeCauldronFluidTransfers() {
        return true;
    }

    default boolean anvilcraft$isIgnited() {
        return false;
    }

    default void anvilcraft$setIgnited(boolean ignited) {
    }
}
