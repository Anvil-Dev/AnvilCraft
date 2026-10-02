package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.frost.FrostSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.frost.IFrostSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.util.ItemResourceHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class FrostSmithingTransfer {
    private FrostSmithingTransfer() {
    }

    public static boolean same(ItemStack first, ItemStack second, RegistryAccess registries) {
        return !first.isEmpty() && !second.isEmpty()
            && ItemResourceHelper.matchesNetworkStack(first.copyWithCount(1), second.copyWithCount(1), registries);
    }

    public static int firstSlot(AbstractContainerMenu menu) {
        return menu instanceof FrostSmithingMenu ? 1 : 0;
    }

    public static List<ItemStack> available(Player player, AbstractContainerMenu menu) {
        List<ItemStack> stacks = new ArrayList<>();
        int start = firstSlot(menu);
        stacks.add(menu.getSlot(start).getItem().copy());
        stacks.add(menu.getSlot(start + 1).getItem().copy());
        for (int slot = 0; slot < 36; slot++) stacks.add(player.getInventory().getItem(slot).copy());
        return stacks;
    }

    public static long count(List<ItemStack> stacks, ItemStack sample, RegistryAccess registries) {
        return stacks.stream().filter(stack -> same(stack, sample, registries)).mapToLong(ItemStack::getCount).sum();
    }

    public static boolean transfer(ServerPlayer player, ItemStack equipment, ItemStack material, List<UUID> targets) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof FrostSmithingMenu) && !(menu instanceof TranscendenceSmithingMenu transcendence
            && transcendence.getMode() == TranscendenceSmithingMenu.Mode.FROST)) {
            return false;
        }
        if (!menu.stillValid(player) || equipment.isEmpty() || equipment.getCount() > equipment.getMaxStackSize()
            || material.getCount() > material.getMaxStackSize() || targets.size() > 64) {
            return false;
        }
        ItemStack template = menu instanceof TranscendenceSmithingMenu transcendence
            ? transcendence.getSelectedTemplate() : menu.getSlot(0).getItem();
        var input = new FrostSmithingRecipeInput(template, equipment, material);
        var recipes = RecipesRecord.getRecipes(player.level());
        boolean valid = Stream.concat(recipes.byType(ModRecipeTypes.PERMUTATION.get()).stream(),
                recipes.byType(ModRecipeTypes.DEFORMATION.get()).stream())
            .anyMatch(holder -> ((IFrostSmithingRecipe) holder.value()).matches(input, player.level()));
        if (!valid) return false;

        int start = firstSlot(menu);
        List<ItemStack> desired = new ArrayList<>();
        for (ItemStack need : List.of(equipment, material)) {
            if (need.isEmpty()) continue;
            ItemStack existing = desired.stream().filter(stack -> same(stack, need, player.registryAccess())).findFirst().orElse(null);
            if (existing == null) desired.add(need.copy());
            else existing.grow(need.getCount());
        }
        for (ItemStack need : desired) {
            long inGrid = count(List.of(menu.getSlot(start).getItem(), menu.getSlot(start + 1).getItem()), need, player.registryAccess());
            need.setCount((int) Math.max(0, need.getCount() - inGrid));
        }
        var restock = StorageServerStub.terminalRestock(player.getUUID(), menu.containerId, targets, desired);
        boolean transferred;
        try {
            transferred = fill(player, menu, equipment, material);
        } finally {
            if (!restock.withdrawn().isEmpty()) StorageServerStub.terminalReturnExcess(player.getUUID(), targets, restock);
        }
        menu.broadcastChanges();
        return transferred;
    }

    private static boolean fill(ServerPlayer player, AbstractContainerMenu menu, ItemStack equipment, ItemStack material) {
        List<ItemStack> pool = available(player, menu);
        ItemStack takenEquipment = take(pool, equipment, player.registryAccess());
        if (takenEquipment.isEmpty()) return false;
        ItemStack takenMaterial = take(pool, material, player.registryAccess());
        if (!material.isEmpty() && takenMaterial.isEmpty()) return false;
        List<ItemStack> inventory = new ArrayList<>(pool.subList(2, pool.size()));
        if (!stow(inventory, pool.get(0), player.registryAccess()) || !stow(inventory, pool.get(1), player.registryAccess())) return false;
        Runnable apply = () -> {
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, inventory.get(slot));
            int start = firstSlot(menu);
            menu.getSlot(start).set(takenEquipment);
            menu.getSlot(start + 1).set(takenMaterial);
            player.getInventory().setChanged();
        };
        if (menu instanceof AdjacentSmithingMenu adjacent) adjacent.runRecipeTransfer(apply);
        else apply.run();
        return true;
    }

    private static ItemStack take(List<ItemStack> pool, ItemStack requested, RegistryAccess registries) {
        if (requested.isEmpty()) return ItemStack.EMPTY;
        int remaining = requested.getCount();
        ItemStack result = ItemStack.EMPTY;
        for (ItemStack stack : pool) {
            if (!same(stack, requested, registries)) continue;
            int amount = Math.min(remaining, stack.getCount());
            if (result.isEmpty()) result = stack.copyWithCount(amount);
            else result.grow(amount);
            stack.shrink(amount);
            remaining -= amount;
            if (remaining == 0) return result;
        }
        return ItemStack.EMPTY;
    }

    private static boolean stow(List<ItemStack> inventory, ItemStack remainder, RegistryAccess registries) {
        for (ItemStack stack : inventory) {
            if (!same(stack, remainder, registries)) continue;
            int amount = Math.min(remainder.getCount(), stack.getMaxStackSize() - stack.getCount());
            stack.grow(amount);
            remainder.shrink(amount);
        }
        for (int slot = 0; slot < inventory.size() && !remainder.isEmpty(); slot++) {
            if (!inventory.get(slot).isEmpty()) continue;
            inventory.set(slot, remainder.copy());
            remainder.setCount(0);
        }
        return remainder.isEmpty();
    }
}
