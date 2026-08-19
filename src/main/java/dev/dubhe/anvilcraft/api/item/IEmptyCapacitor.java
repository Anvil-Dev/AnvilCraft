package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 空电容器物品
 */
public interface IEmptyCapacitor {
    ItemStack getFull(ItemStack empty);
}
