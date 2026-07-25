package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

final class SingleFluidTankHandler extends FluidTank {
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_ENHANCED = "Enhanced";
    private static final String TAG_INFINITE = "Infinite";

    private final int baseCapacity;
    private final int infinityThreshold;
    private final Runnable changeListener;
    private boolean enhanced;
    private boolean infinite;

    SingleFluidTankHandler(int baseCapacity, int infinityThreshold, Runnable changeListener) {
        super(baseCapacity);
        this.baseCapacity = baseCapacity;
        this.infinityThreshold = infinityThreshold;
        this.changeListener = changeListener;
    }

    @Override
    protected void onContentsChanged() {
        this.changeListener.run();
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        if (this.infinite) {
            if (resource.isEmpty()
                || !this.isFluidValid(resource)
                || !FluidStack.isSameFluidSameComponents(this.fluid, resource)) {
                return 0;
            }
            return resource.getAmount();
        }
        if (!this.enhanced && this.getSpace() == 0) return 0;

        boolean reachesInfinity = this.enhanced
            && !resource.isEmpty()
            && this.isFluidValid(resource)
            && (this.fluid.isEmpty() || FluidStack.isSameFluidSameComponents(this.fluid, resource))
            && resource.getAmount() >= this.getSpace();
        if (reachesInfinity && action.simulate()) return resource.getAmount();

        int filled = super.fill(resource, action);
        if (reachesInfinity && action.execute() && this.getFluidAmount() >= this.infinityThreshold) {
            this.infinite = true;
            this.changeListener.run();
            return resource.getAmount();
        }
        return filled;
    }

    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        if (!this.infinite) return super.drain(maxDrain, action);
        return maxDrain <= 0 || this.fluid.isEmpty()
            ? FluidStack.EMPTY
            : this.fluid.copyWithAmount(maxDrain);
    }

    void setEnhanced(boolean enhanced) {
        if (this.enhanced == enhanced) return;
        this.enhanced = enhanced;
        this.infinite = enhanced && this.getFluidAmount() >= this.infinityThreshold;
        super.setCapacity(enhanced ? this.infinityThreshold : this.baseCapacity);
        this.changeListener.run();
    }

    boolean isInfinite() {
        return this.infinite;
    }

    boolean isEnhanced() {
        return this.enhanced;
    }

    @Override
    public FluidTank readFromNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.enhanced = tag.getBoolean(TAG_ENHANCED);
        this.infinite = false;
        super.setCapacity(this.enhanced ? this.infinityThreshold : this.baseCapacity);
        super.readFromNBT(provider, tag);
        this.infinite = this.enhanced
            && tag.getBoolean(TAG_INFINITE)
            && this.getFluidAmount() == this.infinityThreshold;
        this.changeListener.run();
        return this;
    }

    @Override
    public CompoundTag writeToNBT(HolderLookup.Provider provider, CompoundTag tag) {
        super.writeToNBT(provider, tag);
        tag.putBoolean(TAG_ENHANCED, this.enhanced);
        tag.putBoolean(TAG_INFINITE, this.infinite);
        return tag;
    }

    CompoundTag serializeForItem(HolderLookup.Provider provider) {
        return this.serializeDetached(provider, Integer.MAX_VALUE);
    }

    CompoundTag serializeForDrop(HolderLookup.Provider provider) {
        return this.serializeDetached(provider, this.baseCapacity);
    }

    private CompoundTag serializeDetached(HolderLookup.Provider provider, int maxAmount) {
        CompoundTag tag = new CompoundTag();
        if (!this.fluid.isEmpty()) {
            int amount = Math.min(this.fluid.getAmount(), maxAmount);
            tag.put(TAG_FLUID, this.fluid.copyWithAmount(amount).save(provider));
        }
        tag.putBoolean(TAG_ENHANCED, false);
        tag.putBoolean(TAG_INFINITE, false);
        return tag;
    }
}
