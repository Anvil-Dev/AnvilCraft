package dev.dubhe.anvilcraft.util;

import net.minecraft.world.phys.Vec3;

/**
 * 用于实体mixin中获取实体是否被转向环转向的接口
 */
public interface DeflectionEntity {
    default boolean isDeflected() {
        return false;
    }

    default Vec3 getFixedDeltaMovement() {
        return Vec3.ZERO;
    }
}