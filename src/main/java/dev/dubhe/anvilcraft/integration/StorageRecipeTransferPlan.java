package dev.dubhe.anvilcraft.integration;

import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StorageRecipeTransferPlan {
    private StorageRecipeTransferPlan() {
    }

    public record Result(List<ItemStack> inputs, IntList counts, IntList missing) {
    }

    public static Result create(
        List<List<ItemStack>> variants, Map<ItemResource, Long> available, List<StorageServerStub.FluidEntry> fluidEntries
    ) {
        Map<ItemResource, Long> items = new HashMap<>(available);
        Map<FluidResource, Long> fluids = new HashMap<>();
        for (var entry : fluidEntries) fluids.merge(FluidResource.of(entry.icon()), (long) entry.amount(), Long::sum);
        List<ItemStack> inputs = new ArrayList<>(variants.size());
        IntList counts = new IntArrayList(variants.size());
        IntList missing = new IntArrayList();
        for (int index = 0; index < variants.size(); index++) {
            List<ItemStack> options = variants.get(index).stream().filter(stack -> !stack.isEmpty()).toList();
            ItemStack chosen = ItemStack.EMPTY;
            for (ItemStack option : options) {
                if (reserve(items, ItemResource.of(option), 1)) {
                    chosen = option.copyWithCount(1);
                    break;
                }
            }
            if (chosen.isEmpty()) {
                for (ItemStack option : options) {
                    FluidContainer container = container(option);
                    if (container == null || items.getOrDefault(container.empty, 0L) < 1
                        || fluids.getOrDefault(container.fluid, 0L) < container.amount) continue;
                    reserve(items, container.empty, 1);
                    reserve(fluids, container.fluid, container.amount);
                    chosen = option.copyWithCount(1);
                    break;
                }
            }
            if (chosen.isEmpty() && !options.isEmpty()) missing.add(index);
            inputs.add(chosen);
            counts.add(chosen.isEmpty() ? 0 : 1);
        }
        return new Result(List.copyOf(inputs), counts, missing);
    }

    private static <T> boolean reserve(Map<T, Long> resources, T resource, long count) {
        long available = resources.getOrDefault(resource, 0L);
        if (available < count) return false;
        resources.put(resource, available - count);
        return true;
    }

    public record FluidContainer(ItemResource empty, FluidResource fluid, int amount) {
    }

    public static @Nullable FluidContainer container(ItemStack wanted) {
        FluidStack fluid = FluidUtil.getFirstStackContained(wanted.copyWithCount(1));
        if (fluid.isEmpty()) return null;
        var single = new ItemStacksResourceHandler(1);
        single.set(0, ItemResource.of(wanted), 1);
        var handler = ItemAccess.forHandlerIndexStrict(single, 0).getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return null;
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(FluidResource.of(fluid), fluid.getAmount(), transaction) != fluid.getAmount()) return null;
            ItemResource empty = single.getResource(0);
            if (empty.isEmpty() || empty.equals(ItemResource.of(wanted))) return null;
            var filled = new ItemStacksResourceHandler(1);
            filled.set(0, empty, 1);
            var filler = ItemAccess.forHandlerIndexStrict(filled, 0).getCapability(Capabilities.Fluid.ITEM);
            if (filler == null || filler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction) != fluid.getAmount()
                || !filled.getResource(0).equals(ItemResource.of(wanted))) return null;
            return new FluidContainer(empty, FluidResource.of(fluid), fluid.getAmount());
        }
    }
}
