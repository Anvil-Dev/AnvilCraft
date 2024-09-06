package dev.dubhe.anvilcraft.block.state;

import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;

/**
 * 多方块方块状态
 */
public interface MultiplePartBlockState<T> extends StringRepresentable, Comparable<T> {
    int getOffsetX();

    int getOffsetY();

    int getOffsetZ();

    default Vec3i getOffset() {
        return new Vec3i(this.getOffsetX(), this.getOffsetY(), this.getOffsetZ());
    }
}
