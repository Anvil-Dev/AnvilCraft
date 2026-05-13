package dev.dubhe.anvilcraft.util;

import com.google.common.base.Preconditions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class ItemResourceHelper {
    public static ItemStack getStackInSlot(ResourceHandler<ItemResource> handler, int slot) {
        Preconditions.checkElementIndex(slot, handler.size());
        return handler.getResource(slot).toStack(handler.getAmountAsInt(slot));
    }

    public static int getSlotLimit(ResourceHandler<ItemResource> handler, int slot) {
        Preconditions.checkElementIndex(slot, handler.size());
        ItemResource resource = handler.getResource(slot);
        return handler.getCapacityAsInt(slot, resource);
    }

    public static boolean isSlotEmpty(ResourceHandler<ItemResource> handler, int slot) {
        Preconditions.checkElementIndex(slot, handler.size());
        return handler.getAmountAsInt(slot) <= 0;
    }
}
