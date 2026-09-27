package dev.dubhe.anvilcraft.api.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

public interface IImpersonator<T extends Entity> {
    boolean isValidMask(@Nullable Class<?> mask);

    T impersonate(LivingEntity entity);
}
