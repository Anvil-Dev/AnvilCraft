package dev.dubhe.anvilcraft.api.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class EntityHelper {
    @ExpectPlatform
    public static CompoundTag getCustomData(LivingEntity entity) {
        return null;
    }
}
