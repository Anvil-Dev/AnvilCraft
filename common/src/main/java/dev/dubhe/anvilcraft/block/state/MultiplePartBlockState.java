package dev.dubhe.anvilcraft.block.state;

import net.minecraft.core.Vec3i;

/**
 * 多方块方块状态
 */
public interface MultiplePartBlockState {
    int getOffsetX();

    int getOffsetY();

    int getOffsetZ();

    default Vec3i getOffset() {
        return new Vec3i(this.getOffsetX(), this.getOffsetY(), this.getOffsetZ());
    }
}
