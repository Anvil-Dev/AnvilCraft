package dev.dubhe.anvilcraft.api.entity.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

public class EntityHelperImpl {

    public static CompoundTag getCustomData(LivingEntity entity) {
        return entity.getPersistentData();
    }
}
