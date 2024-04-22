package dev.dubhe.anvilcraft.api.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

public class EntityHelper {
    @ExpectPlatform
    public static CompoundTag getCustomData(LivingEntity entity) {
        return null;
    }
}
