package dev.dubhe.anvilcraft.api.fluidtank;

import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 创造模式流体内核：无限存储/供给指定流体。
 * insert 永远返回全部（接受所有流体），
 * extract 永远返回请求量（无限供给已设定的流体）。
 */
public class CreativeFluidHandler extends FluidStacksResourceHandler {

    public CreativeFluidHandler() {
        super(NonNullList.of(FluidStack.EMPTY, FluidStack.EMPTY), Integer.MAX_VALUE);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        // 不管当前存了什么，直接覆盖为指定流体
        FluidStack existing = this.stacks.get(index);
        if (existing.isEmpty() || !FluidResource.of(existing).equals(resource)) {
            this.stacks.set(index, resource.toStack(amount));
        }
        return amount;
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
        // 创造模式储罐永远显示满箱
        return this.capacity;
    }
}
