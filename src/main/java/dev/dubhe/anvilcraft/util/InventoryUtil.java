package dev.dubhe.anvilcraft.util;

import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class InventoryUtil {
    public static BiConsumer<ArrayList<ItemStack>, LivingEntity> compatConsumer = NonNullBiConsumer.noop();

    public static ItemStack getFirstItem(Inventory inventory, Item item) {
        for (ItemStack stack : getItems(inventory)) {
            if (stack.getItem().equals(item)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getFirstItem(Inventory inventory, Supplier<? extends Item> item) {
        for (ItemStack stack : getItems(inventory)) {
            if (stack.getItem().equals(item.get())) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getFirstItem(Inventory inventory, Predicate<ItemStack> filter) {
        for (ItemStack stack : getItems(inventory)) {
            if (filter.test(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static List<ItemStack> getItems(Inventory inventory, Predicate<ItemStack> filter) {
        List<ItemStack> items = getItems(inventory);
        items.removeIf(stack -> !filter.test(stack));
        return items;
    }

    public static List<ItemStack> getItems(Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();

        items.addAll(inventory.offhand);
        items.addAll(inventory.items);
        items.addAll(inventory.armor);

        return items;
    }

    public static boolean hasItem(Inventory inventory, Item item) {
        return !getFirstItem(inventory, item).equals(ItemStack.EMPTY);
    }

    public static boolean hasItem(Inventory inventory, ItemEntry<? extends Item> item) {
        return !getFirstItem(inventory, item).equals(ItemStack.EMPTY);
    }

    public static ItemStack getItemInCompat(LivingEntity entity, Predicate<ItemStack> filter) {
        for (ItemStack stack : getCompatItems(entity)) {
            if (filter.test(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static ArrayList<ItemStack> getCompatItems(LivingEntity living) {
        ArrayList<ItemStack> items = new ArrayList<>();
        compatConsumer.accept(items, living);
        return items;
    }

    public static boolean hasItemInCompat(LivingEntity entity, Predicate<ItemStack> filter) {
        for (ItemStack stack : getCompatItems(entity)) {
            if (filter.test(stack)) {
                return true;
            }
        }

        return false;
    }

    public static ItemStack insertItem(Inventory inventory, ItemStack stack) {
        while (!stack.isEmpty()) {
            int slot = inventory.getSlotWithRemainingSpace(stack);
            if (slot == -1) {
                slot = inventory.getFreeSlot();
            }

            if (slot == -1) {
                return stack;
            }

            int remaining = stack.getMaxStackSize() - inventory.getItem(slot).getCount();
            if (inventory.add(slot, stack.split(remaining)) && inventory.player instanceof ServerPlayer) {
                ((ServerPlayer) inventory.player).connection.send(new ClientboundContainerSetSlotPacket(-2, 0, slot, inventory.getItem(slot)));
            }
        }
        return stack;
    }
}
