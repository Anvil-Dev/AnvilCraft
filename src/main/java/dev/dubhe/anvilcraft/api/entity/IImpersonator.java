package dev.dubhe.anvilcraft.api.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public interface IImpersonator<T extends Entity> {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isValidMask(@Nullable Class<?> mask);

    T impersonate(LivingEntity entity);
}
