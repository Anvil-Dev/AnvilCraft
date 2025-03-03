package dev.dubhe.anvilcraft.util;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class InventoryUtil {
    public static ItemStack getFirstItem(Inventory inventory, Item item) {
        for (ItemStack stack : inventory.items) {
            if (stack.getItem().equals(item)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getFirstItem(Inventory inventory, Supplier<Item> item) {
        for (ItemStack stack : inventory.items) {
            if (stack.getItem().equals(item.get())) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getFirstItem(Inventory inventory, ItemEntry<? extends Item> item) {
        for (ItemStack stack : inventory.items) {
            if (item.isIn(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static NonNullList<ItemStack> getItems(Inventory inventory, Item item) {
        NonNullList<ItemStack> items = NonNullList.of(ItemStack.EMPTY);

        for (ItemStack stack : inventory.items) {
            if (stack.getItem().equals(item)) {
                items.add(stack);
            }
        }

        return items;
    }

    public static boolean hasItem(Inventory inventory, Item item) {
        return !getFirstItem(inventory, item).equals(ItemStack.EMPTY);
    }

    public static boolean hasItem(Inventory inventory, Supplier<Item> item) {
        return !getFirstItem(inventory, item).equals(ItemStack.EMPTY);
    }

    public static boolean hasItem(Inventory inventory, ItemEntry<? extends Item> item) {
        return !getFirstItem(inventory, item).equals(ItemStack.EMPTY);
    }
}
