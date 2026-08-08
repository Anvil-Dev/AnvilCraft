package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 空电容器物品
 */
public interface IEmptyCapacitor extends IChargerChargeable {
    ItemStack getFull(ItemStack empty);

    @Override
    default ItemStack charge(ItemStack input) {
        return this.getFull(input);
    }
}
