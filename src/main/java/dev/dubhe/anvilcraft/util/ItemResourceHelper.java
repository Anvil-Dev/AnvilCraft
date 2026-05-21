package dev.dubhe.anvilcraft.util;

import com.google.common.base.Preconditions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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

    public static ItemStack insertInto(ResourceHandler<ItemResource> handler, int index, ItemStack stack) {
        ItemResource resource = ItemResource.of(stack);
        int remain;
        try (Transaction transaction = Transaction.openRoot()) {
            remain = handler.insert(index, resource, stack.count(), transaction);
            transaction.commit();
        }
        if (remain == 0) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(remain);
    }
}
