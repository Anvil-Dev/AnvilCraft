package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;

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
        @Nullable private BuildingBlockMaterial.Supplied blockMaterial;
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
        ItemStack held = BuildingRodItem.material(player);
        this.offhand = new Source(held, held.getCount(), held::shrink);
        this.fluidStorages = StorageServerStub.buildingFluidSources(player);
        for (ItemStack stack : PocketInventory.carriedItems(player)) {
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
            int remaining = this.reserveItem(material);
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
            int remaining = this.reserveFluid(fluid);
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

    @Nullable
    BuildingRodService.Group reserve(BuildingRodService.Group group, boolean allowMismatch) {
        final int[] before = this.sources.stream().mapToInt(source -> source.reserved).toArray();
        BuildingRodService.Group allocated = new BuildingRodService.Group();
        if (!group.separateContents) allocated.entities.addAll(group.entities);
        allocated.fluids.addAll(group.fluids);
        allocated.materials.addAll(group.materials);
        for (var entry : group.blockMaterials.entrySet()) {
            ItemStack expected = entry.getValue();
            List<BlockSupply> taken = new ArrayList<>();
            int remaining = this.creative ? 0 : this.reserveBlock(expected, true, expected.getCount(), taken, group.separateContents);
            if (this.creative) taken.add(new BlockSupply(expected.copy(), expected.copy(), List.of()));
            if (remaining > 0 && allowMismatch) remaining = this.reserveBlock(expected, false, remaining, taken, group.separateContents);
            if (remaining > 0) {
                for (int i = 0; i < before.length; i++) this.sources.get(i).reserved = before[i];
                return null;
            }
            allocated.blockMaterials.put(entry.getKey(), taken.getFirst().placed());
            taken.forEach(supply -> {
                allocated.materials.add(supply.original());
                allocated.returned.addAll(supply.returned());
            });
            allocated.componentMismatch |= taken.stream()
                .anyMatch(supply -> !ItemStack.isSameItemSameComponents(supply.placed(), expected));
        }
        if (!this.reserve(group.materials)) {
            for (int i = 0; i < before.length; i++) this.sources.get(i).reserved = before[i];
            return null;
        }
        for (var cell : group.cells) {
            List<ItemStack> contents = cell.contents().stream().map(BlockEntityContentAdapter.SlotStack::stack).toList();
            if (this.reserve(contents)) {
                allocated.cells.add(cell);
                allocated.materials.addAll(contents);
            } else if (allowMismatch) {
                BlockState state = cell.state();
                if (state.hasProperty(BlockStateProperties.HAS_BOOK)) state = state.setValue(BlockStateProperties.HAS_BOOK, false);
                if (state.hasProperty(BlockStateProperties.HAS_RECORD)) state = state.setValue(BlockStateProperties.HAS_RECORD, false);
                allocated.cells.add(new BuildingRodService.Cell(cell.pos(), state, cell.config(), List.of()));
                allocated.componentMismatch = true;
                allocated.missingContents = true;
            } else {
                for (int i = 0; i < before.length; i++) this.sources.get(i).reserved = before[i];
                return null;
            }
        }
        if (group.separateContents) {
            for (var entity : group.entities) {
                List<ItemStack> contents = entity.contents().stream().map(EntityBuildAdapter.SlotStack::stack).toList();
                if (this.reserve(contents)) {
                    allocated.entities.add(entity);
                    allocated.materials.addAll(contents);
                } else if (allowMismatch) {
                    allocated.entities.add(new EntityBuildAdapter.Planned(entity.material(), entity.returned(),
                        entity.entityNbt(), List.of(), entity.fluids(), false));
                    allocated.componentMismatch = true;
                    allocated.missingContents = true;
                } else {
                    for (int i = 0; i < before.length; i++) this.sources.get(i).reserved = before[i];
                    return null;
                }
            }
        }
        if (!this.reserve(List.of(), group.fluids)) {
            for (int i = 0; i < before.length; i++) this.sources.get(i).reserved = before[i];
            return null;
        }
        if (!this.creative) {
            allocated.returned.forEach(stack -> this.returned.add(stack.copy()));
            for (ItemStack stack : allocated.blockMaterials.values()) {
                if (stack.is(Items.POWDER_SNOW_BUCKET)) {
                    ItemStack bucket = new ItemStack(Items.BUCKET, stack.getCount());
                    allocated.returned.add(bucket);
                    this.returned.add(bucket.copy());
                }
            }
        }
        return allocated;
    }

    private record BlockSupply(ItemStack original, ItemStack placed, List<ItemStack> returned) {
    }

    private int reserveBlock(ItemStack expected, boolean exact, int remaining, List<BlockSupply> taken, boolean separateContents) {
        for (Source source : this.sources) {
            if (!ItemStack.isSameItem(source.resource, expected) || source.available <= source.reserved) continue;
            if (separateContents && source.blockMaterial == null) {
                source.blockMaterial = BuildingBlockMaterial.separateContents(source.resource, this.player.level());
            }
            var material = separateContents && source.blockMaterial != null
                ? source.blockMaterial : new BuildingBlockMaterial.Supplied(source.resource, List.of());
            ItemStack empty = material.stack();
            if (ItemStack.isSameItemSameComponents(empty, expected) != exact) continue;
            int take = (int) Math.min(remaining, source.available - source.reserved);
            source.reserved += take;
            remaining -= take;
            List<ItemStack> returned = material.contents().stream()
                .map(content -> content.stack().copyWithCount(content.stack().getCount() * take)).toList();
            taken.add(new BlockSupply(source.resource.copyWithCount(take), empty.copyWithCount(take), returned));
            if (remaining == 0) break;
        }
        return remaining;
    }

    private int reserveItem(ItemStack material) {
        int remaining = material.getCount();
        for (Source source : this.sources) {
            if (!ItemStack.isSameItemSameComponents(source.resource, material)) continue;
            int take = (int) Math.min(remaining, source.available - source.reserved);
            source.reserved += take;
            remaining -= take;
            if (remaining == 0) break;
        }
        return remaining;
    }

    private int reserveFluid(FluidStack fluid) {
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
        return Math.max(0, remaining);
    }

    List<Component> missing(List<BuildingRodService.Group> groups) {
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (var group : groups) {
            List<ItemStack> required = new ArrayList<>(group.materials);
            group.cells.forEach(cell -> cell.contents().forEach(content -> required.add(content.stack())));
            if (group.separateContents) {
                group.entities.forEach(entity -> entity.contents().forEach(content -> required.add(content.stack())));
            }
            required.addAll(group.blockMaterials.values());
            for (ItemStack material : required) {
                ItemStack combined = items.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, material))
                    .findFirst().orElse(null);
                if (combined == null) items.add(material.copy());
                else combined.grow(material.getCount());
            }
            for (FluidStack fluid : group.fluids) {
                FluidStack combined = fluids.stream().filter(stack -> FluidStack.isSameFluidSameComponents(stack, fluid))
                    .findFirst().orElse(null);
                if (combined == null) fluids.add(fluid.copy());
                else combined.grow(fluid.getAmount());
            }
        }
        List<Component> lines = new ArrayList<>();
        for (ItemStack material : items) {
            int missing = this.reserveItem(material);
            if (missing > 0) lines.add(material.getHoverName().copy().append(" ×" + missing));
        }
        for (FluidStack fluid : fluids) {
            int missing = this.reserveFluid(fluid);
            if (missing > 0) lines.add(fluid.getHoverName().copy().append(" ×" + missing + " mB"));
        }
        return lines;
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
