package dev.dubhe.anvilcraft.api.fluid;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

public class LargeCauldronFluidHandler implements IFluidHandler, INBTSerializable<CompoundTag> {
    public static final int TANK_COUNT = 8;
    public static final int TANK_CAPACITY = 64 * FluidType.BUCKET_VOLUME;
    public static final int TOTAL_CAPACITY = TANK_COUNT * TANK_CAPACITY;
    private final Runnable changeListener;
    private final FluidTank[] tanks = new FluidTank[TANK_COUNT];
    private boolean suppressChanges;

    public LargeCauldronFluidHandler(Runnable changeListener) {
        this.changeListener = changeListener;
        for (int i = 0; i < this.tanks.length; i++) {
            this.tanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    if (!LargeCauldronFluidHandler.this.suppressChanges) {
                        LargeCauldronFluidHandler.this.changeListener.run();
                    }
                }
            };
        }
    }

    @Override
    public int getTanks() {
        return TANK_COUNT;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank >= 0 && tank < TANK_COUNT ? this.tanks[tank].getFluid() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return TANK_CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (stack.isEmpty()) return false;
        int matching = this.findFluidType(stack);
        return matching < 0 || FluidStack.isSameFluidSameComponents(this.tanks[matching].getFluid(), stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return this.fill(resource, action, false);
    }

    private int fill(FluidStack resource, FluidAction action, boolean atBottom) {
        if (resource.isEmpty()) return 0;
        int matching = this.findFluidType(resource);
        if (matching >= 0
            && !FluidStack.isSameFluidSameComponents(this.tanks[matching].getFluid(), resource)) {
            return 0;
        }

        List<FluidStack> layers = this.nonEmptyFluids();
        if (matching < 0 && layers.size() >= TANK_COUNT) return 0;
        int stored = matching < 0 ? 0 : this.tanks[matching].getFluidAmount();
        int accepted = Math.min(resource.getAmount(), TANK_CAPACITY - stored);
        if (accepted <= 0) return 0;
        if (action.simulate()) return accepted;

        FluidStack filled = matching < 0
            ? resource.copyWithAmount(accepted)
            : this.tanks[matching].getFluid().copyWithAmount(stored + accepted);
        if (matching >= 0) layers.remove(matching);
        if (atBottom) {
            layers.add(0, filled);
        } else if (matching >= 0) {
            layers.add(matching, filled);
        } else {
            layers.add(filled);
        }
        this.setFluids(layers);
        return accepted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return this.drainFirstMatching(
            resource, action, this.layerOrder(DrainOrder.TOP, TOTAL_CAPACITY));
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        List<Integer> order = this.layerOrder(DrainOrder.TOP, TOTAL_CAPACITY);
        return order.isEmpty() ? FluidStack.EMPTY : this.drainLayer(order.getFirst(), maxDrain, action);
    }

    public IFluidHandler bottomAccess() {
        return new LayeredView(DrainOrder.BOTTOM, TOTAL_CAPACITY, true);
    }

    public IFluidHandler topAccess() {
        return new LayeredView(DrainOrder.TOP, TOTAL_CAPACITY, false);
    }

    public IFluidHandler sideAccess(int accessibleAmount) {
        return new LayeredView(DrainOrder.HEIGHT, accessibleAmount, false);
    }

    public FluidStack drainStoredFluid(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        int layer = this.findFluidType(resource);
        if (layer < 0 || !FluidStack.isSameFluidSameComponents(this.tanks[layer].getFluid(), resource)) {
            return FluidStack.EMPTY;
        }
        return this.drainLayer(layer, resource.getAmount(), action);
    }

    private FluidStack drainFirstMatching(
        FluidStack resource, FluidAction action, List<Integer> order
    ) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        for (int layer : order) {
            FluidStack stored = this.getFluidInTank(layer);
            if (FluidStack.isSameFluidSameComponents(stored, resource)) {
                return this.drainLayer(layer, resource.getAmount(), action);
            }
        }
        return FluidStack.EMPTY;
    }

    private FluidStack drainLayer(int layer, int maxDrain, FluidAction action) {
        FluidStack stored = this.getFluidInTank(layer);
        if (stored.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
        int drained = Math.min(maxDrain, stored.getAmount());
        FluidStack result = stored.copyWithAmount(drained);
        if (action.execute()) {
            List<FluidStack> layers = this.nonEmptyFluids();
            int remaining = stored.getAmount() - drained;
            if (remaining == 0) {
                layers.remove(layer);
            } else {
                layers.set(layer, stored.copyWithAmount(remaining));
            }
            this.setFluids(layers);
        }
        return result;
    }

    private int findFluidType(FluidStack resource) {
        for (int i = 0; i < this.activeLayers(); i++) {
            if (this.tanks[i].getFluid().getFluid() == resource.getFluid()) return i;
        }
        return -1;
    }

    private int activeLayers() {
        for (int i = 0; i < TANK_COUNT; i++) {
            if (this.tanks[i].isEmpty()) return i;
        }
        return TANK_COUNT;
    }

    private int layerAtHeight(int accessibleAmount) {
        int active = this.activeLayers();
        if (active == 0) return -1;
        int target = Math.min(Math.max(1, accessibleAmount), this.getTotalAmount());
        int amount = 0;
        for (int i = 0; i < active; i++) {
            amount += this.tanks[i].getFluidAmount();
            if (amount >= target) return i;
        }
        return active - 1;
    }

    private List<Integer> layerOrder(DrainOrder order, int accessibleAmount) {
        int active = this.activeLayers();
        List<Integer> result = new ArrayList<>(active);
        if (order == DrainOrder.BOTTOM) {
            for (int i = 0; i < active; i++) result.add(i);
            return result;
        }
        int start = order == DrainOrder.TOP ? active - 1 : this.layerAtHeight(accessibleAmount);
        for (int i = start; i >= 0; i--) result.add(i);
        return result;
    }

    private List<FluidStack> nonEmptyFluids() {
        List<FluidStack> result = new ArrayList<>(TANK_COUNT);
        for (FluidTank tank : this.tanks) {
            if (!tank.isEmpty()) result.add(tank.getFluid().copy());
        }
        return result;
    }

    public List<FluidStack> copyFluids() {
        List<FluidStack> result = new ArrayList<>(TANK_COUNT);
        for (FluidTank tank : this.tanks) result.add(tank.getFluid().copy());
        return result;
    }

    public void setFluids(List<FluidStack> fluids) {
        List<FluidStack> compact = new ArrayList<>(TANK_COUNT);
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) continue;
            compact.add(fluid.copyWithAmount(Math.min(fluid.getAmount(), TANK_CAPACITY)));
            if (compact.size() == TANK_COUNT) break;
        }
        this.suppressChanges = true;
        try {
            for (int i = 0; i < this.tanks.length; i++) {
                this.tanks[i].setFluid(i < compact.size() ? compact.get(i) : FluidStack.EMPTY);
            }
        } finally {
            this.suppressChanges = false;
        }
        this.changeListener.run();
    }

    public int getTotalAmount() {
        int total = 0;
        for (FluidTank tank : this.tanks) total += tank.getFluidAmount();
        return total;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < this.tanks.length; i++) {
            if (this.tanks[i].isEmpty()) continue;
            CompoundTag entry = this.tanks[i].writeToNBT(provider, new CompoundTag());
            entry.putInt("Tank", i);
            list.add(entry);
        }
        CompoundTag result = new CompoundTag();
        result.put("Tanks", list);
        return result;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.suppressChanges = true;
        try {
            for (FluidTank tank : this.tanks) tank.setFluid(FluidStack.EMPTY);
            ListTag list = tag.getList("Tanks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int tank = entry.getInt("Tank");
                if (tank < 0 || tank >= TANK_COUNT) continue;
                this.tanks[tank].readFromNBT(provider, entry);
            }
        } finally {
            this.suppressChanges = false;
        }
        this.setFluids(this.nonEmptyFluids());
    }

    private enum DrainOrder {
        TOP,
        BOTTOM,
        HEIGHT
    }

    private class LayeredView implements IFluidHandler {
        private final DrainOrder drainOrder;
        private final int accessibleAmount;
        private final boolean fillAtBottom;

        private LayeredView(DrainOrder drainOrder, int accessibleAmount, boolean fillAtBottom) {
            this.drainOrder = drainOrder;
            this.accessibleAmount = accessibleAmount;
            this.fillAtBottom = fillAtBottom;
        }

        private List<Integer> order() {
            return LargeCauldronFluidHandler.this.layerOrder(this.drainOrder, this.accessibleAmount);
        }

        @Override
        public int getTanks() {
            return TANK_COUNT;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            List<Integer> order = this.order();
            return tank >= 0 && tank < order.size()
                ? LargeCauldronFluidHandler.this.getFluidInTank(order.get(tank))
                : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return LargeCauldronFluidHandler.this.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return LargeCauldronFluidHandler.this.fill(resource, action, this.fillAtBottom);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return LargeCauldronFluidHandler.this.drainFirstMatching(resource, action, this.order());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            List<Integer> order = this.order();
            return order.isEmpty()
                ? FluidStack.EMPTY
                : LargeCauldronFluidHandler.this.drainLayer(order.getFirst(), maxDrain, action);
        }
    }
}
