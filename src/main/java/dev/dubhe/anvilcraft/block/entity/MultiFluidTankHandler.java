package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.fluid.GasDisplayFillProvider;
import dev.dubhe.anvilcraft.api.fluid.InfiniteFluidSource;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.RangedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class MultiFluidTankHandler implements
    ResourceHandler<FluidResource>, ValueIOSerializable, GasDisplayFillProvider, InfiniteFluidSource {
    private static final Codec<List<FluidStack>> FLUIDS_CODEC = FluidStack.OPTIONAL_CODEC.listOf();
    private static final Codec<List<Boolean>> FLAGS_CODEC = Codec.BOOL.listOf();

    private final int baseCapacity;
    private final int infinityThreshold;
    private final Runnable changeListener;
    private final List<StoredFluid> fluids = new ArrayList<>();
    private final TankJournal snapshotJournal = new TankJournal();
    private boolean enhanced;

    MultiFluidTankHandler(int baseCapacity, int infinityThreshold, Runnable changeListener) {
        this.baseCapacity = baseCapacity;
        this.infinityThreshold = infinityThreshold;
        this.changeListener = changeListener;
    }

    @Override
    public int size() {
        return this.enhanced ? this.fluids.size() + 1 : Math.max(1, this.fluids.size());
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, this.size());
        return index < this.fluids.size() ? FluidResource.of(this.fluids.get(index).fluid()) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, this.size());
        if (index >= this.fluids.size()) {
            return 0;
        }
        StoredFluid stored = this.fluids.get(index);
        return stored.suppliesInfinitely() ? Integer.MAX_VALUE : stored.fluid().getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, this.size());
        if (this.enhanced) {
            return index < this.fluids.size() && this.fluids.get(index).suppliesInfinitely()
                ? Integer.MAX_VALUE : this.infinityThreshold;
        }
        if (this.fluids.isEmpty()) return this.baseCapacity;
        if (index >= this.fluids.size()) return this.getRemainingCapacity();
        StoredFluid stored = this.fluids.get(index);
        return index == 0 ? stored.fluid().getAmount() + this.getRemainingCapacity() : stored.fluid().getAmount();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, this.size());
        return !resource.isEmpty();
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        return this.insert(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int index = this.findFluid(resource);
        if (!this.enhanced) {
            int inserted = Math.min(amount, this.getRemainingCapacity());
            if (inserted <= 0) return 0;
            this.snapshotJournal.updateSnapshots(transaction);
            if (index < 0) {
                this.fluids.add(new StoredFluid(resource.toStack(inserted), false));
            } else {
                this.fluids.get(index).fluid().grow(inserted);
            }
            return inserted;
        }

        int accepted = this.getEnhancedAccepted(resource, amount, index);
        if (accepted <= 0) return 0;
        this.snapshotJournal.updateSnapshots(transaction);
        if (index < 0) {
            boolean infinite = accepted >= this.infinityThreshold && mayBeInfinite(resource.toStack(accepted));
            this.fluids.add(new StoredFluid(resource.toStack(Math.min(accepted, this.infinityThreshold)), infinite));
        } else {
            StoredFluid stored = this.fluids.get(index);
            long total = (long) stored.fluid().getAmount() + accepted;
            stored.fluid().setAmount((int) Math.min(total, this.infinityThreshold));
            if (total >= this.infinityThreshold && mayBeInfinite(stored.fluid())) stored.setInfinite(true);
        }
        return accepted;
    }

    /// 计算增强态下本次可接收的量。
    ///
    /// 本次输入若能把该流体推到阈值，且该流体未被 {@link ModFluidTags#CANNOT_BE_INFINITE} 排除，
    /// 就会转为无限供给，从而能全部接收；否则存量在阈值处封顶，
    /// 必须按实际剩余空间返回接收量，否则管道会误以为这些流体已经入罐而多抽走。
    private int getEnhancedAccepted(FluidResource resource, int amount, int index) {
        if (index >= 0 && this.fluids.get(index).suppliesInfinitely()) return amount;
        int stored = index < 0 ? 0 : this.fluids.get(index).fluid().getAmount();
        long space = (long) this.infinityThreshold - stored;
        if (space <= 0) return 0;
        if (mayBeInfinite(resource.toStack(amount)) && amount >= space) return amount;
        return (int) Math.min(amount, space);
    }

    ResourceHandler<FluidResource> accessLayer(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return this;
        }
        return RangedResourceHandler.ofSingleIndex(this, this.findFluid(FluidResource.of(fluid)));
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (index >= this.fluids.size() || !this.getResource(index).equals(resource)) return 0;
        return this.extractStored(index, amount, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int index = this.findFluid(resource);
        return index < 0 ? 0 : this.extractStored(index, amount, transaction);
    }

    private int extractStored(int index, int amount, TransactionContext transaction) {
        StoredFluid stored = this.fluids.get(index);
        if (stored.suppliesInfinitely()) return amount;
        int extracted = Math.min(amount, stored.fluid().getAmount());
        if (extracted <= 0) return 0;
        this.snapshotJournal.updateSnapshots(transaction);
        stored.fluid().shrink(extracted);
        if (stored.fluid().isEmpty()) this.fluids.remove(index);
        return extracted;
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
        for (StoredFluid stored : this.fluids) amount += stored.fluid().getAmount();
        return amount;
    }

    @Override
    public float gasDisplayFill(FluidStack gas) {
        long renderAmount = this.enhanced ? Math.max(this.getTotalAmount(), this.infinityThreshold) : this.baseCapacity;
        if (renderAmount <= 0) return 0;
        int index = this.findFluid(FluidResource.of(gas));
        int stored = index < 0 ? 0 : this.fluids.get(index).fluid().getAmount();
        return (float) Math.min(1, (double) stored / renderAmount);
    }

    boolean isEnhanced() {
        return this.enhanced;
    }

    boolean isInfinite(FluidStack fluid) {
        int index = this.findFluid(FluidResource.of(fluid));
        return index >= 0 && this.fluids.get(index).suppliesInfinitely();
    }

    /// 判断该流体是否允许进入无限状态；空流体按允许处理
    private static boolean mayBeInfinite(FluidStack stack) {
        return stack.isEmpty() || !stack.is(ModFluidTags.CANNOT_BE_INFINITE);
    }

    void setEnhanced(boolean enhanced) {
        if (this.enhanced == enhanced) return;
        this.enhanced = enhanced;
        for (StoredFluid stored : this.fluids) {
            stored.setInfinite(enhanced
                               && stored.fluid().getAmount() >= this.infinityThreshold
                               && mayBeInfinite(stored.fluid()));
        }
        this.changeListener.run();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("Fluids", MultiFluidTankHandler.FLUIDS_CODEC, this.copyFluids());
        output.store("Infinite", MultiFluidTankHandler.FLAGS_CODEC, this.fluids.stream().map(StoredFluid::infinite).toList());
        output.putBoolean("Enhanced", this.enhanced);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.fluids.clear();
        this.enhanced = input.getBooleanOr("Enhanced", false);
        List<FluidStack> loaded = input.read("Fluids", MultiFluidTankHandler.FLUIDS_CODEC).orElse(List.of());
        List<Boolean> infinite = input.read("Infinite", MultiFluidTankHandler.FLAGS_CODEC).orElse(List.of());
        for (int index = 0; index < loaded.size(); index++) {
            FluidStack fluid = loaded.get(index);
            if (fluid.isEmpty()) continue;
            int amount = Math.min(fluid.getAmount(), this.infinityThreshold);
            boolean isInfinite = this.enhanced
                                 && index < infinite.size()
                                 && infinite.get(index)
                                 && amount == this.infinityThreshold
                                 && mayBeInfinite(fluid);
            this.fluids.add(new StoredFluid(fluid.copyWithAmount(amount), isInfinite));
        }
        if (!this.enhanced) this.clearInfinite();
        this.changeListener.run();
    }

    void serializeForItem(ValueOutput output) {
        this.serializeDetached(output, Long.MAX_VALUE);
    }

    void serializeForDrop(ValueOutput output) {
        this.serializeDetached(output, this.baseCapacity);
    }

    private void serializeDetached(ValueOutput output, long maxAmount) {
        List<FluidStack> detachedFluids = new ArrayList<>();
        long remaining = maxAmount;
        for (StoredFluid stored : this.fluids) {
            if (remaining <= 0) break;
            int amount = (int) Math.min(stored.fluid().getAmount(), remaining);
            if (amount <= 0) continue;
            detachedFluids.add(stored.fluid().copyWithAmount(amount));
            remaining -= amount;
        }
        output.store("Fluids", MultiFluidTankHandler.FLUIDS_CODEC, detachedFluids);
        output.store("Infinite", MultiFluidTankHandler.FLAGS_CODEC, detachedFluids.stream().map(ignored -> false).toList());
        output.putBoolean("Enhanced", false);
    }

    /// 取消扩容时只清掉无限化标记，保留已存流体，避免修改容量导致存量凭空减少
    private void clearInfinite() {
        for (StoredFluid stored : this.fluids) {
            stored.setInfinite(false);
        }
    }

    private int getRemainingCapacity() {
        return Math.max(0, this.baseCapacity - (int) this.getTotalAmount());
    }

    private int findFluid(FluidResource resource) {
        for (int index = 0; index < this.fluids.size(); index++) {
            if (FluidResource.of(this.fluids.get(index).fluid()).equals(resource)) return index;
        }
        return -1;
    }

    private record TankState(List<StoredFluid> fluids, boolean enhanced) {
    }

    private class TankJournal extends SnapshotJournal<TankState> {
        @Override
        protected TankState createSnapshot() {
            return new TankState(MultiFluidTankHandler.this.fluids.stream().map(StoredFluid::copy).toList(),
                MultiFluidTankHandler.this.enhanced);
        }

        @Override
        protected void revertToSnapshot(TankState snapshot) {
            MultiFluidTankHandler.this.fluids.clear();
            MultiFluidTankHandler.this.fluids.addAll(snapshot.fluids().stream().map(StoredFluid::copy).toList());
            MultiFluidTankHandler.this.enhanced = snapshot.enhanced();
        }

        @Override
        protected void onRootCommit(TankState originalState) {
            MultiFluidTankHandler.this.changeListener.run();
        }
    }

    private static final class StoredFluid {
        private final FluidStack fluid;
        private boolean infinite;

        private StoredFluid(FluidStack fluid, boolean infinite) {
            this.fluid = fluid;
            this.infinite = infinite;
        }

        private FluidStack fluid() {
            return this.fluid;
        }

        private boolean infinite() {
            return this.infinite;
        }

        private void setInfinite(boolean infinite) {
            this.infinite = infinite;
        }

        /// 无限供给是否生效。
        ///
        /// 每次按当前流体标签重新判定，而不是只信任缓存的 {@link #infinite} 标记：
        /// 数据包重载把该流体加入 {@link ModFluidTags#CANNOT_BE_INFINITE} 后，
        /// 已加载的储罐必须立刻恢复普通行为，无需重新加载存档。
        private boolean suppliesInfinitely() {
            return this.infinite && mayBeInfinite(this.fluid);
        }

        private StoredFluid copy() {
            return new StoredFluid(this.fluid.copy(), this.infinite);
        }
    }
}
