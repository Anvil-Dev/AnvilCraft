package dev.dubhe.anvilcraft.api.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

public class EntityHelper {
    public static CompoundTag getCustomData(LivingEntity entity) {
        return entity.getPersistentData();
    }
}
