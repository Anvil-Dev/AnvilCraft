package dev.dubhe.anvilcraft.block.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

final class MultiFluidTankHandler implements IFluidHandler, INBTSerializable<CompoundTag> {
    private static final String TAG_FLUIDS = "Fluids";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_ENHANCED = "Enhanced";
    private static final String TAG_INFINITE = "Infinite";

    private final int baseCapacity;
    private final int infinityThreshold;
    private final Runnable changeListener;
    private final List<StoredFluid> fluids = new ArrayList<>();
    private boolean enhanced;

    MultiFluidTankHandler(int baseCapacity, int infinityThreshold, Runnable changeListener) {
        this.baseCapacity = baseCapacity;
        this.infinityThreshold = infinityThreshold;
        this.changeListener = changeListener;
    }

    @Override
    public int getTanks() {
        if (this.enhanced) {
            return this.fluids.size() + 1;
        }
        return Math.max(1, this.fluids.size());
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank >= 0 && tank < this.fluids.size()
            ? this.fluids.get(tank).fluid()
            : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < 0 || tank >= this.getTanks()) return 0;
        if (this.enhanced) return this.infinityThreshold;
        if (this.fluids.isEmpty()) return this.baseCapacity;

        StoredFluid stored = this.fluids.get(tank);
        if (tank != 0) return stored.fluid().getAmount();
        return stored.fluid().getAmount() + this.getRemainingCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank >= 0 && tank < this.getTanks() && !stack.isEmpty();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || resource.getAmount() <= 0) return 0;
        int index = this.findFluid(resource);
        if (this.enhanced) {
            if (action.execute()) {
                this.fillEnhanced(resource, index);
            }
            return resource.getAmount();
        }

        int accepted = Math.min(resource.getAmount(), this.getRemainingCapacity());
        if (accepted <= 0) return 0;
        if (action.execute()) {
            if (index < 0) {
                this.fluids.add(new StoredFluid(resource.copyWithAmount(accepted), false));
            } else {
                StoredFluid stored = this.fluids.get(index);
                stored.fluid().grow(accepted);
            }
            this.changeListener.run();
        }
        return accepted;
    }

    private void fillEnhanced(FluidStack resource, int index) {
        if (index < 0) {
            int amount = Math.min(resource.getAmount(), this.infinityThreshold);
            this.fluids.add(new StoredFluid(resource.copyWithAmount(amount), amount == this.infinityThreshold));
            this.changeListener.run();
            return;
        }

        StoredFluid stored = this.fluids.get(index);
        if (stored.infinite()) return;
        long result = (long) stored.fluid().getAmount() + resource.getAmount();
        stored.fluid().setAmount((int) Math.min(result, this.infinityThreshold));
        if (result >= this.infinityThreshold) {
            stored.infinite(true);
        }
        this.changeListener.run();
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || resource.getAmount() <= 0) return FluidStack.EMPTY;
        int index = this.findFluid(resource);
        return index < 0 ? FluidStack.EMPTY : this.drain(index, resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || this.fluids.isEmpty()) return FluidStack.EMPTY;
        return this.drain(0, maxDrain, action);
    }

    private FluidStack drain(int index, int maxDrain, FluidAction action) {
        StoredFluid stored = this.fluids.get(index);
        int drained = stored.infinite() ? maxDrain : Math.min(maxDrain, stored.fluid().getAmount());
        FluidStack result = stored.fluid().copyWithAmount(drained);
        if (action.execute() && !stored.infinite()) {
            stored.fluid().shrink(drained);
            if (stored.fluid().isEmpty()) {
                this.fluids.remove(index);
            }
            this.changeListener.run();
        }
        return result;
    }

    List<FluidStack> copyFluids() {
        return this.fluids.stream().map(stored -> stored.fluid().copy()).toList();
    }

    long getTotalAmount() {
        long amount = 0;
        for (StoredFluid stored : this.fluids) {
            amount += stored.fluid().getAmount();
        }
        return amount;
    }

    boolean isEnhanced() {
        return this.enhanced;
    }

    boolean isInfinite(FluidStack fluid) {
        int index = this.findFluid(fluid);
        return index >= 0 && this.fluids.get(index).infinite();
    }

    void setEnhanced(boolean enhanced) {
        if (this.enhanced == enhanced) return;
        this.enhanced = enhanced;
        for (StoredFluid stored : this.fluids) {
            stored.infinite(enhanced && stored.fluid().getAmount() >= this.infinityThreshold);
        }
        this.changeListener.run();
    }

    private int getRemainingCapacity() {
        return Math.max(0, this.baseCapacity - (int) this.getTotalAmount());
    }

    private int findFluid(FluidStack resource) {
        for (int i = 0; i < this.fluids.size(); i++) {
            if (FluidStack.isSameFluidSameComponents(this.fluids.get(i).fluid(), resource)) return i;
        }
        return -1;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_ENHANCED, this.enhanced);
        ListTag fluidsTag = new ListTag();
        for (StoredFluid stored : this.fluids) {
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.put(TAG_FLUID, stored.fluid().save(provider));
            fluidTag.putBoolean(TAG_INFINITE, stored.infinite());
            fluidsTag.add(fluidTag);
        }
        tag.put(TAG_FLUIDS, fluidsTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.fluids.clear();
        this.enhanced = tag.getBoolean(TAG_ENHANCED);
        ListTag fluidsTag = tag.getList(TAG_FLUIDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < fluidsTag.size(); i++) {
            CompoundTag fluidTag = fluidsTag.getCompound(i);
            FluidStack fluid = FluidStack.parseOptional(provider, fluidTag.getCompound(TAG_FLUID));
            if (fluid.isEmpty()) continue;
            int amount = Math.min(fluid.getAmount(), this.infinityThreshold);
            boolean infinite = this.enhanced
                && fluidTag.getBoolean(TAG_INFINITE)
                && amount == this.infinityThreshold;
            this.fluids.add(new StoredFluid(fluid.copyWithAmount(amount), infinite));
        }
        this.changeListener.run();
    }

    CompoundTag serializeForItem(HolderLookup.Provider provider) {
        return this.serializeDetached(provider, Long.MAX_VALUE);
    }

    CompoundTag serializeForDrop(HolderLookup.Provider provider) {
        return this.serializeDetached(provider, this.baseCapacity);
    }

    private CompoundTag serializeDetached(HolderLookup.Provider provider, long maxAmount) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_ENHANCED, false);
        ListTag fluidsTag = new ListTag();
        long remaining = maxAmount;
        for (StoredFluid stored : this.fluids) {
            if (remaining <= 0) break;
            int amount = (int) Math.min(stored.fluid().getAmount(), remaining);
            if (amount <= 0) continue;

            CompoundTag fluidTag = new CompoundTag();
            fluidTag.put(TAG_FLUID, stored.fluid().copyWithAmount(amount).save(provider));
            fluidTag.putBoolean(TAG_INFINITE, false);
            fluidsTag.add(fluidTag);
            remaining -= amount;
        }
        tag.put(TAG_FLUIDS, fluidsTag);
        return tag;
    }

    @Accessors(fluent = true, chain = false)
    @AllArgsConstructor
    @Data
    private static final class StoredFluid {
        private final FluidStack fluid;
        @Setter(AccessLevel.PRIVATE)
        private boolean infinite;
    }
}
