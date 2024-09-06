package dev.dubhe.anvilcraft.api;

import net.minecraft.world.item.ItemStack;

/**
 * 带有展示物品的方块实体
 */
public interface IHasDisplayItem {
    void updateDisplayItem(ItemStack stack);
}
