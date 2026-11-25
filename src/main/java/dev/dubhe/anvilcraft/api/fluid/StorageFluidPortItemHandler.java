package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/// 仓储流体端口物品的流体能力，让端口像储罐一样参与通用流体交互。
public class StorageFluidPortItemHandler extends ItemAccessResourceHandler<FluidResource> {
    public StorageFluidPortItemHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        FluidStack fluid = StorageFluidPortItemHandler.storedFluid(accessResource);
        return fluid.isEmpty() ? FluidResource.EMPTY : FluidResource.of(fluid);
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return StorageFluidPortItemHandler.storedFluid(accessResource).getAmount();
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
        ItemStack stack = accessResource.toStack(1);
        int current = StorageFluidPortBlockEntity.containedFluid(stack, registries).getAmount();
        int delta = newAmount - current;
        if (delta > 0) {
            StorageFluidPortBlockEntity.insertIntoItem(stack, newResource.toStack(delta), registries);
        } else if (delta < 0) {
            StorageFluidPortBlockEntity.drainItem(stack, newResource.toStack(-delta), registries);
        }
        return ItemResource.of(stack);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return StorageFluidPortBlockEntity.CAPACITY_MB;
    }

    private static FluidStack storedFluid(ItemResource accessResource) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        if (registries == null) {
            return FluidStack.EMPTY;
        }
        return StorageFluidPortBlockEntity.containedFluid(accessResource.toStack(1), registries);
    }
}