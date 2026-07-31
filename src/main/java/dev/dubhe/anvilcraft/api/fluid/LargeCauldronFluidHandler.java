package dev.dubhe.anvilcraft.api.fluid;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LargeCauldronFluidHandler implements ResourceHandler<FluidResource>, ValueIOSerializable {
    public static final int TANK_COUNT = 8;
    public static final int TANK_CAPACITY = 64 * FluidType.BUCKET_VOLUME;
    public static final int TOTAL_CAPACITY = LargeCauldronFluidHandler.TANK_COUNT * LargeCauldronFluidHandler.TANK_CAPACITY;
    private static final Codec<List<FluidStack>> FLUIDS_CODEC = FluidStack.OPTIONAL_CODEC.listOf();

    private final Runnable changeListener;
    private final List<FluidStack> fluids = new ArrayList<>(LargeCauldronFluidHandler.TANK_COUNT);
    private final FluidsJournal snapshotJournal = new FluidsJournal();

    public LargeCauldronFluidHandler(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    @Override
    public int size() {
        return LargeCauldronFluidHandler.TANK_COUNT;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
        return index < this.fluids.size() ? FluidResource.of(this.fluids.get(index)) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
        return index < this.fluids.size() ? this.fluids.get(index).getAmount() : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
        return LargeCauldronFluidHandler.TANK_CAPACITY;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
        if (resource.isEmpty()) return false;
        return this.findFluid(resource) >= 0 || this.fluids.size() < LargeCauldronFluidHandler.TANK_COUNT;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
        return this.insert(resource, amount, false, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        return this.insert(resource, amount, false, transaction);
    }

    private int insert(
        FluidResource resource,
        int amount,
        boolean atBottom,
        TransactionContext transaction
    ) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int matching = this.findFluid(resource);
        if (matching < 0 && this.fluids.size() >= LargeCauldronFluidHandler.TANK_COUNT) return 0;

        int stored = matching < 0 ? 0 : this.fluids.get(matching).getAmount();
        int inserted = Math.min(amount, LargeCauldronFluidHandler.TANK_CAPACITY - stored);
        if (inserted <= 0) return 0;
        this.snapshotJournal.updateSnapshots(transaction);

        FluidStack filled = resource.toStack(stored + inserted);
        if (matching >= 0) this.fluids.remove(matching);
        if (atBottom) {
            this.fluids.addFirst(filled);
        } else if (matching >= 0) {
            this.fluids.add(matching, filled);
        } else {
            this.fluids.add(filled);
        }
        return inserted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return this.extractLayer(index, resource, amount, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        for (int layer = this.fluids.size() - 1; layer >= 0; layer--) {
            if (this.getResource(layer).equals(resource)) {
                return this.extractLayer(layer, resource, amount, transaction);
            }
        }
        return 0;
    }

    private int extractLayer(
        int layer,
        FluidResource resource,
        int amount,
        TransactionContext transaction
    ) {
        if (layer >= this.fluids.size() || !this.getResource(layer).equals(resource)) return 0;
        int extracted = Math.min(amount, this.fluids.get(layer).getAmount());
        if (extracted <= 0) return 0;
        this.snapshotJournal.updateSnapshots(transaction);

        int remaining = this.fluids.get(layer).getAmount() - extracted;
        if (remaining == 0) {
            this.fluids.remove(layer);
        } else {
            this.fluids.set(layer, resource.toStack(remaining));
        }
        return extracted;
    }

    public ResourceHandler<FluidResource> bottomAccess() {
        return new LayeredView(DrainOrder.BOTTOM, LargeCauldronFluidHandler.TOTAL_CAPACITY, true);
    }

    public ResourceHandler<FluidResource> topAccess() {
        return new LayeredView(DrainOrder.TOP, LargeCauldronFluidHandler.TOTAL_CAPACITY, false);
    }

    public ResourceHandler<FluidResource> sideAccess(int accessibleAmount) {
        return new LayeredView(DrainOrder.HEIGHT, accessibleAmount, false);
    }

    public FluidStack getFluidInTank(int tank) {
        FluidResource resource = this.getResource(tank);
        return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack(this.getAmountAsInt(tank));
    }

    public List<FluidStack> copyFluids() {
        List<FluidStack> result = new ArrayList<>(LargeCauldronFluidHandler.TANK_COUNT);
        for (FluidStack fluid : this.fluids) result.add(fluid.copy());
        while (result.size() < LargeCauldronFluidHandler.TANK_COUNT) result.add(FluidStack.EMPTY);
        return result;
    }

    public void setFluids(List<FluidStack> fluids) {
        this.fluids.clear();
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) continue;
            this.fluids.add(fluid.copyWithAmount(Math.min(fluid.getAmount(), LargeCauldronFluidHandler.TANK_CAPACITY)));
            if (this.fluids.size() == LargeCauldronFluidHandler.TANK_COUNT) break;
        }
        this.changeListener.run();
    }

    public int getTotalAmount() {
        int total = 0;
        for (FluidStack fluid : this.fluids) total += fluid.getAmount();
        return total;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("Fluids", LargeCauldronFluidHandler.FLUIDS_CODEC, this.fluids);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.setFluids(input.read("Fluids", LargeCauldronFluidHandler.FLUIDS_CODEC).orElse(List.of()));
    }

    private int findFluid(FluidResource resource) {
        for (int layer = 0; layer < this.fluids.size(); layer++) {
            if (this.getResource(layer).equals(resource)) return layer;
        }
        return -1;
    }

    private int layerAtHeight(int accessibleAmount) {
        if (this.fluids.isEmpty()) return -1;
        int target = Math.min(Math.max(1, accessibleAmount), this.getTotalAmount());
        int amount = 0;
        for (int layer = 0; layer < this.fluids.size(); layer++) {
            amount += this.fluids.get(layer).getAmount();
            if (amount >= target) return layer;
        }
        return this.fluids.size() - 1;
    }

    private List<Integer> layerOrder(DrainOrder order, int accessibleAmount) {
        List<Integer> result = new ArrayList<>(this.fluids.size());
        if (order == DrainOrder.BOTTOM) {
            for (int layer = 0; layer < this.fluids.size(); layer++) result.add(layer);
            return result;
        }
        int start = order == DrainOrder.TOP ? this.fluids.size() - 1 : this.layerAtHeight(accessibleAmount);
        for (int layer = start; layer >= 0; layer--) result.add(layer);
        return result;
    }

    private enum DrainOrder {
        TOP,
        BOTTOM,
        HEIGHT
    }

    private class FluidsJournal extends SnapshotJournal<List<FluidStack>> {
        @Override
        protected List<FluidStack> createSnapshot() {
            return LargeCauldronFluidHandler.this.copyFluids();
        }

        @Override
        protected void revertToSnapshot(List<FluidStack> snapshot) {
            LargeCauldronFluidHandler.this.fluids.clear();
            for (FluidStack fluid : snapshot) {
                if (!fluid.isEmpty()) LargeCauldronFluidHandler.this.fluids.add(fluid.copy());
            }
        }

        @Override
        protected void onRootCommit(List<FluidStack> originalState) {
            LargeCauldronFluidHandler.this.changeListener.run();
        }
    }

    private class LayeredView implements ResourceHandler<FluidResource> {
        private final DrainOrder drainOrder;
        private final int accessibleAmount;
        private final boolean fillAtBottom;

        private LayeredView(DrainOrder drainOrder, int accessibleAmount, boolean fillAtBottom) {
            this.drainOrder = drainOrder;
            this.accessibleAmount = accessibleAmount;
            this.fillAtBottom = fillAtBottom;
        }

        @Override
        public int size() {
            return LargeCauldronFluidHandler.TANK_COUNT;
        }

        @Override
        public FluidResource getResource(int index) {
            Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
            List<Integer> order = LargeCauldronFluidHandler.this.layerOrder(this.drainOrder, this.accessibleAmount);
            return index < order.size()
                ? LargeCauldronFluidHandler.this.getResource(order.get(index))
                : FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
            List<Integer> order = LargeCauldronFluidHandler.this.layerOrder(this.drainOrder, this.accessibleAmount);
            return index < order.size() ? LargeCauldronFluidHandler.this.getAmountAsLong(order.get(index)) : 0;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
            return LargeCauldronFluidHandler.TANK_CAPACITY;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
            return LargeCauldronFluidHandler.this.isValid(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
            return this.insert(resource, amount, transaction);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            return LargeCauldronFluidHandler.this.insert(resource, amount, this.fillAtBottom, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            Objects.checkIndex(index, LargeCauldronFluidHandler.TANK_COUNT);
            List<Integer> order = LargeCauldronFluidHandler.this.layerOrder(this.drainOrder, this.accessibleAmount);
            return index < order.size()
                ? LargeCauldronFluidHandler.this.extractLayer(order.get(index), resource, amount, transaction)
                : 0;
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            for (int layer : LargeCauldronFluidHandler.this.layerOrder(this.drainOrder, this.accessibleAmount)) {
                if (LargeCauldronFluidHandler.this.getResource(layer).equals(resource)) {
                    return LargeCauldronFluidHandler.this.extractLayer(layer, resource, amount, transaction);
                }
            }
            return 0;
        }
    }
}
