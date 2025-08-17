package dev.dubhe.anvilcraft.api.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;

public interface INegativeShapeBlock<T> extends IHammerRemovable {
    Class<T> getBlockType();
}
