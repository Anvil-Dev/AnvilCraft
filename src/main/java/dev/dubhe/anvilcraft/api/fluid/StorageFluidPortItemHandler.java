package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/// 仓储流体端口物品的 {@link IFluidHandlerItem} 实现，让端口像储罐一样参与通用流体交互。
public class StorageFluidPortItemHandler implements IFluidHandlerItem {
    private final ItemStack container;

    public StorageFluidPortItemHandler(ItemStack container) {
        this.container = container.copyWithCount(1);
    }

    @Override
    public ItemStack getContainer() {
        return this.container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        return StorageFluidPortBlockEntity.containedFluid(this.container, registries);
    }

    @Override
    public int getTankCapacity(int tank) {
        return StorageFluidPortBlockEntity.CAPACITY_MB;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return !stack.isEmpty();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        // 模拟时不能动到真正的容器物品，只能在一件的副本上试
        ItemStack target = action.execute() ? this.container : this.container.copyWithCount(1);
        return StorageFluidPortBlockEntity.insertIntoItem(target, resource, registries);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        ItemStack target = action.execute() ? this.container : this.container.copyWithCount(1);
        int drained = StorageFluidPortBlockEntity.drainItem(target, resource, registries);
        return drained <= 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack content = this.getFluidInTank(0);
        return content.isEmpty() ? FluidStack.EMPTY : this.drain(content.copyWithAmount(maxDrain), action);
    }
}