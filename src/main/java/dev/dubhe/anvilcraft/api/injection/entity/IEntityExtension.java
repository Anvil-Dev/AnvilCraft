package dev.dubhe.anvilcraft.api.injection.entity;

import net.minecraft.world.phys.Vec3;

/// 用于实体mixin中获取实体是否被转向环转向的接口
public interface IEntityExtension {
    default boolean anvilcraft$isDeflected() {
        return false;
    }

    default Vec3 anvilcraft$getFixedDeltaMovement() {
        return Vec3.ZERO;
    }
}
