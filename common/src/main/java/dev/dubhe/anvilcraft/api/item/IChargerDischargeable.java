package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;

public interface IChargerDischargeable {
    ItemStack discharge(ItemStack input);
}
