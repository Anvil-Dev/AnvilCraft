package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 可使用放电器放电的物品
 */
public interface IChargerDischargeable {
    ItemStack discharge(ItemStack input);
}
