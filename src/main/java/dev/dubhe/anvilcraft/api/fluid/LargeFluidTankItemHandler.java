package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

/// 大型流体储罐物品的流体能力。
///
/// <p>槽位数按上限开，实际只返回已存流体；容量按整罐计，写入时由物品自身按真实剩余空间截断。</p>
public class LargeFluidTankItemHandler extends ItemAccessResourceHandler<FluidResource> {
    /// 单个大型储罐能容纳的流体种类上限，实际数量由物品数据决定
    private static final int MAX_FLUIDS = 16;

    public LargeFluidTankItemHandler(ItemAccess itemAccess) {
        super(itemAccess, LargeFluidTankItemHandler.MAX_FLUIDS);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        FluidStack fluid = LargeFluidTankItemHandler.storedFluid(accessResource, index);
        return fluid.isEmpty() ? FluidResource.EMPTY : FluidResource.of(fluid);
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return LargeFluidTankItemHandler.storedFluid(accessResource, index).getAmount();
    }

    @Override
    protected ItemResource update(
        ItemResource accessResource,
        int index,
        FluidResource newResource,
        int newAmount
    ) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        if (registries == null) {
            return ItemResource.EMPTY;
        }
        // 只改物品数据：这里可能位于调用方的事务中，走静态方法会再开根事务
        ItemStack stack = accessResource.toStack(1);
        List<FluidStack> fluids = new ArrayList<>(
            LargeFluidTankBlockEntity.containedFluids(stack, registries));
        fluids.removeIf(FluidStack::isEmpty);
        FluidStack target = newResource.toStack(newAmount);
        if (target.isEmpty()) {
            if (index >= 0 && index < fluids.size()) {
                fluids.remove(index);
            }
        } else if (index >= 0 && index < fluids.size()) {
            fluids.set(index, target);
        } else {
            fluids.add(target);
        }
        LargeFluidTankBlockEntity.writeItemFluids(stack, fluids, registries);
        return ItemResource.of(stack);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return LargeFluidTankBlockEntity.BASE_CAPACITY;
    }

    private static FluidStack storedFluid(ItemResource accessResource, int index) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        if (registries == null) {
            return FluidStack.EMPTY;
        }
        List<FluidStack> fluids =
            LargeFluidTankBlockEntity.containedFluids(accessResource.toStack(1), registries);
        return index >= 0 && index < fluids.size() ? fluids.get(index) : FluidStack.EMPTY;
    }
}