package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.item.ItemEntity;

public interface IDiscardableItemEntity {
    static IDiscardableItemEntity castFromItemEntity(ItemEntity itemEntity) {
        return (IDiscardableItemEntity) itemEntity;
    }

    boolean anvilcraft$getDiscarded();
}
