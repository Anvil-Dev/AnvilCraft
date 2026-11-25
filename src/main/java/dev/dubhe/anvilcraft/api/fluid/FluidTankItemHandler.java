package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/// 流体储罐物品的流体能力。
///
/// <p>有了它，储罐与其它模组的物品态储罐一样走通用交互，不必在每个界面里单独分支。</p>
public class FluidTankItemHandler extends ItemAccessResourceHandler<FluidResource> {
    public FluidTankItemHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        FluidStack fluid = FluidTankItemHandler.storedFluid(accessResource);
        return fluid.isEmpty() ? FluidResource.EMPTY : FluidResource.of(fluid);
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return FluidTankItemHandler.storedFluid(accessResource).getAmount();
    }

    @Override
    protected ItemResource update(
        ItemResource accessResource,
        int index,
        FluidResource newResource,
        int newAmount
    ) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        // 只改物品数据：这里可能位于调用方的事务中，走静态方法会再开根事务
        ItemStack stack = accessResource.toStack(1);
        FluidTankBlockEntity.writeItemFluid(stack, newResource.toStack(newAmount), registries);
        return ItemResource.of(stack);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return FluidTankBlockEntity.BASE_CAPACITY;
    }

    private static FluidStack storedFluid(ItemResource accessResource) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        return FluidTankBlockEntity.containedFluid(accessResource.toStack(1), registries);
    }
}