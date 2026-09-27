package dev.dubhe.anvilcraft.api.fluidtank;

import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;

/**
 * 创造模式流体内核：无限存储/供给指定流体。
 * insert 接受并丢弃流体，不改变玩家配置的流体种类；
 * extract 永远返回请求量（无限供给已设定的流体）。
 */
public class CreativeFluidHandler extends FluidStacksResourceHandler {

    public CreativeFluidHandler() {
        super(NonNullList.of(FluidStack.EMPTY, FluidStack.EMPTY), Integer.MAX_VALUE);
    }

    public List<FluidStack> getStacks() {
        return this.copyToList().stream().map(FluidStack::copy).toList();
    }

    public void replaceStacks(List<FluidStack> stacks) {
        NonNullList<FluidStack> copied = NonNullList.withSize(this.size(), FluidStack.EMPTY);
        for (int i = 0; i < copied.size() && i < stacks.size(); i++) {
            copied.set(i, stacks.get(i).copy());
        }
        this.setStacks(copied);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        return resource.isEmpty() ? 0 : Math.max(0, amount);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return this.insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        FluidStack existing = this.stacks.get(index);
        if (existing.isEmpty()) return 0;
        if (!FluidResource.of(existing).equals(resource)) return 0;
        return amount;
    }

    @Override
    public long getAmountAsLong(int index) {
        // 已配置的流体保持满量，空储罐保持为空汇。
        return this.getResource(index).isEmpty() ? 0 : this.capacity;
    }
}
