package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;

/** 同一服务端任务内先分配全部材料，预检完成后再统一扣除。 */
public final class BuildingMaterials {
    private final List<Source> sources = new ArrayList<>();
    private final boolean creative;
    private final ServerPlayer player;
    private final List<ItemStack> returned = new ArrayList<>();
    private final Source offhand;
    private final List<UUID> fluidStorages;
    private final List<StoredFluid> storedFluids = new ArrayList<>();

    private static final class StoredFluid {
        private final UUID storage;
        private final FluidStack fluid;
        private int reserved;

        private StoredFluid(UUID storage, FluidStack fluid) {
            this.storage = storage;
            this.fluid = fluid.copyWithAmount(1);
        }
    }

    private static final class Source {
        private final ItemStack resource;
        private final long available;
        private final IntConsumer extract;
        private int reserved;

        private Source(ItemStack resource, long available, IntConsumer extract) {
            this.resource = resource.copy();
            this.available = available;
            this.extract = extract;
        }
    }

    public BuildingMaterials(ServerPlayer player) {
        this.creative = player.isCreative();
        this.player = player;
        ItemStack held = player.getOffhandItem();
        this.offhand = new Source(held, held.getCount(), held::shrink);
        this.fluidStorages = StorageServerStub.buildingFluidSources(player);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack != held) this.sources.add(new Source(stack, stack.getCount(), stack::shrink));
        }
        for (UnlimitedItemStacksResourceHandler handler : StorageServerStub.buildingMaterialSources(player)) {
            for (int slot = 0; slot < handler.size(); slot++) {
                int index = slot;
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    this.sources.add(new Source(
                        stack, handler.getAmountAsLong(slot), amount -> handler.extractUnlimited(index, amount, false)
                    ));
                }
            }
        }
        if (!held.isEmpty()) this.sources.add(this.offhand);
    }

    public boolean reserve(List<ItemStack> materials) {
        if (this.creative) return true;
        int[] before = this.sources.stream().mapToInt(source -> source.reserved).toArray();
        for (ItemStack material : materials) {
            int remaining = material.getCount();
            for (Source source : this.sources) {
                if (!ItemStack.isSameItemSameComponents(source.resource, material)) continue;
                int take = (int) Math.min(remaining, source.available - source.reserved);
                source.reserved += take;
                remaining -= take;
                if (remaining == 0) break;
            }
            if (remaining > 0) {
                for (int i = 0; i < before.length; i++) this.sources.get(i).reserved = before[i];
                return false;
            }
        }
        return true;
    }

    public boolean reserve(List<ItemStack> materials, List<FluidStack> fluids) {
        if (this.creative) return true;
        int[] before = this.sources.stream().mapToInt(source -> source.reserved).toArray();
        int returnCount = this.returned.size();
        int[] fluidsBefore = this.storedFluids.stream().mapToInt(source -> source.reserved).toArray();
        if (!this.reserve(materials)) return false;
        for (FluidStack fluid : fluids) {
            int remaining = fluid.getAmount();
            for (Source source : this.sources) {
                if (source == this.offhand) continue;
                remaining = this.reserveContainer(source, fluid, remaining);
                if (remaining <= 0) break;
            }
            for (UUID storage : this.fluidStorages) {
                if (remaining <= 0) break;
                StoredFluid source = this.storedFluids.stream().filter(value -> value.storage.equals(storage)
                    && FluidStack.isSameFluidSameComponents(value.fluid, fluid)).findFirst().orElse(null);
                if (source == null) {
                    source = new StoredFluid(storage, fluid);
                    this.storedFluids.add(source);
                }
                int available = StoragePortManager.drain(storage, fluid, source.reserved + remaining, true);
                int take = Math.min(remaining, Math.max(0, available - source.reserved));
                source.reserved += take;
                remaining -= take;
            }
            remaining = this.reserveContainer(this.offhand, fluid, remaining);
            if (remaining > 0) {
                for (int i = 0; i < before.length; i++) this.sources.get(i).reserved = before[i];
                for (int i = 0; i < this.storedFluids.size(); i++) {
                    this.storedFluids.get(i).reserved = i < fluidsBefore.length ? fluidsBefore[i] : 0;
                }
                this.returned.subList(returnCount, this.returned.size()).clear();
                return false;
            }
        }
        return true;
    }

    private int reserveContainer(Source source, FluidStack fluid, int remaining) {
        while (remaining > 0 && source.available > source.reserved) {
            ItemStack container = source.resource.copyWithCount(1);
            IFluidHandlerItem handler = container.getCapability(Capabilities.FluidHandler.ITEM);
            if (handler == null) break;
            FluidStack drained = handler.drain(fluid.copyWithAmount(remaining), IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty() || !FluidStack.isSameFluidSameComponents(drained, fluid)) break;
            source.reserved++;
            remaining -= drained.getAmount();
            this.returned.add(handler.getContainer().copy());
        }
        return remaining;
    }

    public boolean consume() {
        for (StoredFluid source : this.storedFluids) {
            if (StoragePortManager.drain(source.storage, source.fluid, source.reserved, true) < source.reserved) return false;
        }
        List<StoredFluid> drained = new ArrayList<>();
        for (StoredFluid source : this.storedFluids) {
            int amount = StoragePortManager.drain(source.storage, source.fluid, source.reserved);
            StoredFluid receipt = new StoredFluid(source.storage, source.fluid);
            receipt.reserved = amount;
            drained.add(receipt);
            if (amount == source.reserved) continue;
            for (StoredFluid refund : drained) {
                while (refund.reserved > 0) {
                    var handler = StoragePortManager.findRefillTarget(refund.storage, refund.fluid);
                    if (handler == null) break;
                    int filled = handler.fill(refund.fluid.copyWithAmount(refund.reserved), IFluidHandler.FluidAction.EXECUTE);
                    if (filled <= 0) break;
                    refund.reserved -= filled;
                }
            }
            return false;
        }
        for (Source source : this.sources) {
            if (source.reserved > 0) source.extract.accept(source.reserved);
        }
        for (ItemStack stack : this.returned) this.player.getInventory().placeItemBackInInventory(stack);
        return true;
    }
}
