package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 可使用充电器充电的物品
 */
public interface IChargerChargeable {
    ItemStack charge(ItemStack input);
}
