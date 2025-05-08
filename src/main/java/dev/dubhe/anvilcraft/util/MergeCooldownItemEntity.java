package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.item.ItemEntity;

/**
 * 用于给ItemEntity设置合并冷却
 */
public interface MergeCooldownItemEntity {
    default void setMergeCooldown(@SuppressWarnings("unused") int cooldown) {
    }

    static MergeCooldownItemEntity castFromItemEntity(ItemEntity itemEntity) {
        return ((MergeCooldownItemEntity) itemEntity);
    }
}
