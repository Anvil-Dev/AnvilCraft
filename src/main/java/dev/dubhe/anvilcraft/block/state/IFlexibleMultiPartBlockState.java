package dev.dubhe.anvilcraft.block.state;

import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;

/// 多方块方块状态
public interface IFlexibleMultiPartBlockState<E, T extends Comparable<T>> extends StringRepresentable, Comparable<E> {
    int getOffsetX(T value);

    int getOffsetY(T value);

    int getOffsetZ(T value);

    boolean isMain();

    default Vec3i getOffset(T value) {
        return new Vec3i(this.getOffsetX(value), this.getOffsetY(value), this.getOffsetZ(value));
    }
}
