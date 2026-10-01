package dev.dubhe.anvilcraft.integration;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public interface TerminalItemInventory extends ResourceHandler<ItemResource> {
    ItemStack getStackInSlot(int slot);

    void setStackInSlot(int slot, ItemStack stack);
}
