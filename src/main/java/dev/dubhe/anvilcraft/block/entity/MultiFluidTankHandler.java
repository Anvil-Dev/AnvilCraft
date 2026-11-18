package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.GasDisplayFillProvider;
import dev.dubhe.anvilcraft.api.fluid.InfiniteFluidSource;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
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

final class MultiFluidTankHandler implements IFluidHandler, INBTSerializable<CompoundTag>, GasDisplayFillProvider, InfiniteFluidSource {
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
        if (tank < 0 || tank >= this.fluids.size()) {
            return FluidStack.EMPTY;
        }
        StoredFluid stored = this.fluids.get(tank);
        return stored.suppliesInfinitely() ? stored.fluid().copyWithAmount(Integer.MAX_VALUE) : stored.fluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < 0 || tank >= this.getTanks()) return 0;
        if (this.enhanced) {
            return tank < this.fluids.size() && this.fluids.get(tank).suppliesInfinitely()
                ? Integer.MAX_VALUE : this.infinityThreshold;
        }
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
            int accepted = this.getEnhancedAccepted(resource, index);
            if (accepted <= 0) return 0;
            if (action.execute()) this.fillEnhanced(resource, index, accepted);
            return accepted;
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

    /**
     * 计算增强态下本次可接收的量。
     *
     * <p>本次输入若能把该流体推到阈值，且该流体未被 {@link ModFluidTags#CANNOT_BE_INFINITE} 排除，
     * 就会转为无限供给，从而能全部接收；否则存量在阈值处封顶，
     * 必须按实际剩余空间返回接收量，否则管道会误以为这些流体已经入罐而多抽走。
     */
    private int getEnhancedAccepted(FluidStack resource, int index) {
        if (index >= 0 && this.fluids.get(index).suppliesInfinitely()) return resource.getAmount();
        int stored = index < 0 ? 0 : this.fluids.get(index).fluid().getAmount();
        long space = (long) this.infinityThreshold - stored;
        if (space <= 0) return 0;
        if (mayBeInfinite(resource) && resource.getAmount() >= space) return resource.getAmount();
        return (int) Math.min(resource.getAmount(), space);
    }

    private void fillEnhanced(FluidStack resource, int index, int accepted) {
        if (index < 0) {
            boolean infinite = accepted >= this.infinityThreshold && mayBeInfinite(resource);
            int stored = Math.min(accepted, this.infinityThreshold);
            this.fluids.add(new StoredFluid(resource.copyWithAmount(stored), infinite));
            this.changeListener.run();
            return;
        }

        StoredFluid stored = this.fluids.get(index);
        if (stored.suppliesInfinitely()) return;
        long result = (long) stored.fluid().getAmount() + accepted;
        stored.fluid().setAmount((int) Math.min(result, this.infinityThreshold));
        if (result >= this.infinityThreshold && mayBeInfinite(stored.fluid())) {
            stored.infinite(true);
        }
        this.changeListener.run();
    }

    /**
     * 判断该流体是否允许进入无限状态
     *
     * @param stack 待判断的流体
     * @return 该流体不在 {@link ModFluidTags#CANNOT_BE_INFINITE} 中时为 true；空流体按允许处理
     */
    private static boolean mayBeInfinite(FluidStack stack) {
        return stack.isEmpty() || !stack.is(ModFluidTags.CANNOT_BE_INFINITE);
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
        int drained = stored.suppliesInfinitely() ? maxDrain : Math.min(maxDrain, stored.fluid().getAmount());
        FluidStack result = stored.fluid().copyWithAmount(drained);
        if (action.execute() && !stored.suppliesInfinitely()) {
            stored.fluid().shrink(drained);
            if (stored.fluid().isEmpty()) {
                this.fluids.remove(index);
            }
            this.changeListener.run();
        }
        return result;
    }

    @Override
    public List<FluidStack> getInfiniteFluids() {
        List<FluidStack> result = new ArrayList<>();
        for (StoredFluid stored : this.fluids) {
            if (stored.suppliesInfinitely()) {
                result.add(stored.fluid().copy());
            }
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

    /**
     * 与大型储罐渲染器完全相同的显示口径：增强态按 {@code max(总存量, 无限阈值)}、
     * 普通态按基础容量换算该气体的填充率。
     *
     * <p>不能用"该气体存量 / 各储罐容量之和"代替：增强态的 {@link #getTanks()} 会多报一个
     * 接纳新流体的空槽，容量求和为 {@code (流体种数 + 1) × 无限阈值}，会把管道内的气体
     * 渲染得比储罐里更透明（单种无限气体为 0.5 对 1.0）。
     */
    @Override
    public float gasDisplayFill(FluidStack gas) {
        long renderAmount = this.enhanced
            ? Math.max(this.getTotalAmount(), this.infinityThreshold)
            : this.baseCapacity;
        if (renderAmount <= 0) {
            return 0.0f;
        }
        int index = this.findFluid(gas);
        int stored = index < 0 ? 0 : this.fluids.get(index).fluid().getAmount();
        return (float) Math.min(1.0, (double) stored / renderAmount);
    }

    boolean isEnhanced() {
        return this.enhanced;
    }

    boolean isInfinite(FluidStack fluid) {
        int index = this.findFluid(fluid);
        return index >= 0 && this.fluids.get(index).suppliesInfinitely();
    }

    void setEnhanced(boolean enhanced) {
        if (this.enhanced == enhanced) return;
        this.enhanced = enhanced;
        for (StoredFluid stored : this.fluids) {
            stored.infinite(enhanced
                            && stored.fluid().getAmount() >= this.infinityThreshold
                            && mayBeInfinite(stored.fluid()));
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
                               && amount == this.infinityThreshold
                               && mayBeInfinite(fluid);
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

        /**
         * 无限供给是否生效。
         *
         * <p>每次按当前流体标签重新判定，而不是只信任缓存的 {@link #infinite} 标记：
         * 数据包重载把该流体加入 {@link ModFluidTags#CANNOT_BE_INFINITE} 后，
         * 已加载的储罐必须立刻恢复普通行为，无需重新加载存档。
         */
        private boolean suppliesInfinitely() {
            return this.infinite && mayBeInfinite(this.fluid);
        }
    }
}
