package dev.dubhe.anvilcraft.integration.jei.transfer;

import dev.dubhe.anvilcraft.integration.StorageRecipeTransferPlan;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.util.ItemResourceHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.common.transfer.RecipeTransferUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TerminalJeiTransferPlan {
    private TerminalJeiTransferPlan() {
    }

    public record Result(List<ItemStack> desiredInventory, List<IRecipeSlotView> missing) {
    }

    public static Result create(Player player, List<Slot> crafting, List<Slot> inventory, List<IRecipeSlotView> inputs,
                                IStackHelper helper, StorageServerStub.TerminalSnapshot snapshot, boolean maximum) {
        Map<Slot, ItemStack> available = new LinkedHashMap<>();
        Map<Integer, ItemStack> representatives = new HashMap<>();
        Map<Object, Long> craftingCounts = new HashMap<>();
        Map<Object, Long> inventoryCounts = new HashMap<>();
        for (Slot slot : crafting) {
            addActual(slot, available, representatives, craftingCounts, helper);
        }
        for (Slot slot : inventory) {
            addActual(slot, available, representatives, inventoryCounts, helper);
        }
        List<ItemStack> supplies = new ArrayList<>(snapshot.items());
        addFilledContainers(supplies, inventory, inputs, snapshot.fluids());
        int virtualIndex = 1000000;
        for (ItemStack stack : supplies) {
            if (stack.isEmpty()) continue;
            var slot = new Slot(new SimpleContainer(1), 0, 0, 0);
            slot.index = virtualIndex++;
            available.put(slot, stack.copy());
            representatives.put(slot.index, stack.copyWithCount(1));
        }
        var operations = RecipeTransferUtil.getRecipeTransferOperations(helper, available, inputs, crafting);
        Map<Object, Long> required = new LinkedHashMap<>();
        for (var operation : operations.results) {
            ItemStack chosen = representatives.get(operation.inventorySlotId());
            Slot target = crafting.stream().filter(slot -> slot.index == operation.craftingSlotId()).findFirst().orElseThrow();
            int amount = maximum ? Math.min(chosen.getMaxStackSize(), target.getMaxStackSize(chosen)) : 1;
            required.merge(helper.getUidForStack(chosen, UidContext.Recipe), (long) amount, Long::sum);
        }
        // An incomplete snapshot cannot rule out variants outside its scan window.
        if (!snapshot.complete()) {
            for (IRecipeSlotView missing : operations.missingItems) {
                int index = inputs.indexOf(missing);
                Set<Object> seen = new HashSet<>();
                missing.getItemStacks().filter(stack -> !stack.isEmpty()).forEach(stack -> {
                    Object uid = helper.getUidForStack(stack, UidContext.Recipe);
                    if (!seen.add(uid)) return;
                    int amount = maximum ? Math.min(stack.getMaxStackSize(), crafting.get(index).getMaxStackSize(stack)) : 1;
                    required.merge(uid, (long) amount, Long::sum);
                });
            }
        }
        List<ItemStack> desired = new ArrayList<>();
        for (var entry : required.entrySet()) {
            Object uid = entry.getKey();
            long missing = Math.max(0, entry.getValue() - craftingCounts.getOrDefault(uid, 0L) - inventoryCounts.getOrDefault(uid, 0L));
            for (ItemStack supply : supplies) {
                if (missing == 0) break;
                if (!helper.getUidForStack(supply, UidContext.Recipe).equals(uid)) continue;
                int take = (int) Math.min(missing, supply.getCount());
                addDesired(player, desired, supply, take);
                missing -= take;
            }
            if (missing > 0 && !snapshot.complete()) {
                ItemStack fallback = inputs.stream().flatMap(IRecipeSlotView::getItemStacks)
                    .filter(stack -> !stack.isEmpty() && helper.getUidForStack(stack, UidContext.Recipe).equals(uid))
                    .findFirst().orElse(ItemStack.EMPTY);
                if (!fallback.isEmpty()) addDesired(player, desired, fallback, (int) Math.min(Integer.MAX_VALUE, missing));
            }
        }
        return new Result(List.copyOf(desired), List.copyOf(operations.missingItems));
    }

    private static void addActual(Slot slot, Map<Slot, ItemStack> available, Map<Integer, ItemStack> representatives,
                                  Map<Object, Long> counts, IStackHelper helper) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || slot.isFake()) return;
        available.put(slot, stack.copy());
        representatives.put(slot.index, stack.copyWithCount(1));
        counts.merge(helper.getUidForStack(stack, UidContext.Recipe), (long) stack.getCount(), Long::sum);
    }

    private static void addDesired(Player player, List<ItemStack> desired, ItemStack resource, int extra) {
        if (extra <= 0) return;
        RegistryAccess registries = player.registryAccess();
        for (ItemStack stack : desired) {
            if (ItemResourceHelper.matchesNetworkStack(stack.copyWithCount(1), resource.copyWithCount(1), registries)) {
                stack.setCount((int) Math.min(Integer.MAX_VALUE, (long) stack.getCount() + extra));
                return;
            }
        }
        long baseline = 0;
        for (int index = 0; index < 36; index++) {
            ItemStack actual = player.getInventory().getItem(index);
            if (ItemResourceHelper.matchesNetworkStack(actual.copyWithCount(1), resource.copyWithCount(1), registries)) {
                baseline += actual.getCount();
            }
        }
        desired.add(resource.copyWithCount((int) Math.min(Integer.MAX_VALUE, baseline + extra)));
    }

    private static void addFilledContainers(List<ItemStack> supplies, List<Slot> inventory, List<IRecipeSlotView> inputs,
                                            List<StorageServerStub.FluidEntry> fluids) {
        Map<ItemResource, Long> emptyCounts = new HashMap<>();
        for (ItemStack stack : supplies) emptyCounts.merge(ItemResource.of(stack), (long) stack.getCount(), Long::sum);
        for (Slot slot : inventory) {
            if (!slot.getItem().isEmpty()) emptyCounts.merge(ItemResource.of(slot.getItem()), (long) slot.getItem().getCount(), Long::sum);
        }
        Map<FluidResource, Long> fluidCounts = new HashMap<>();
        for (var fluid : fluids) fluidCounts.merge(FluidResource.of(fluid.icon()), (long) fluid.amount(), Long::sum);
        Set<ItemResource> seen = new HashSet<>();
        for (IRecipeSlotView input : inputs) {
            for (ItemStack variant : input.getItemStacks().filter(stack -> !stack.isEmpty()).toList()) {
                if (!seen.add(ItemResource.of(variant))) continue;
                var container = StorageRecipeTransferPlan.container(variant);
                if (container == null) continue;
                long count = Math.min(emptyCounts.getOrDefault(container.empty(), 0L),
                    fluidCounts.getOrDefault(container.fluid(), 0L) / container.amount());
                if (count <= 0) continue;
                int amount = (int) Math.min(Integer.MAX_VALUE, count);
                supplies.add(variant.copyWithCount(amount));
                emptyCounts.merge(container.empty(), -(long) amount, Long::sum);
                fluidCounts.merge(container.fluid(), -(long) amount * container.amount(), Long::sum);
            }
        }
    }
}
