package dev.dubhe.anvilcraft.util;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ItemResourceHelper {
    /** Compares network snapshots, including components whose nested stacks use identity equality. */
    public static boolean matchesNetworkStack(ItemStack first, ItemStack second, RegistryAccess registries) {
        if (ItemStack.matches(first, second)) return true;
        if (first.isEmpty() || second.isEmpty() || first.getItem() != second.getItem() || first.getCount() != second.getCount()
            || first.getComponents().size() != second.getComponents().size()) return false;
        for (TypedDataComponent<?> component : first.getComponents()) {
            if (!matchesNetworkComponent(component, second, registries)) return false;
        }
        return true;
    }

    private static <T> boolean matchesNetworkComponent(
        TypedDataComponent<T> component, ItemStack other, RegistryAccess registries
    ) {
        T value = other.get(component.type());
        if (value == null) return false;
        if (component.value().equals(value)) return true;
        var first = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        var second = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            component.type().streamCodec().encode(first, component.value());
            component.type().streamCodec().encode(second, value);
            return ByteBufUtil.equals(first, second);
        } finally {
            first.release();
            second.release();
        }
    }

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
