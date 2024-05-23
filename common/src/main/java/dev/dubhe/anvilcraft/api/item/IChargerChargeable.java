package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;

public interface IChargerChargeable {
    ItemStack charge(ItemStack input);
}
