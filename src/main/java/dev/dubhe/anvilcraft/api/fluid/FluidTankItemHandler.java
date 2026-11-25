package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/// 流体储罐物品的 {@link IFluidHandlerItem} 实现。
///
/// <p>有了它，储罐和其它模组的物品态储罐一样走上通用交互逻辑，不必再在每个界面里单独分支。</p>
public class FluidTankItemHandler implements IFluidHandlerItem {
    private final ItemStack container;

    public FluidTankItemHandler(ItemStack container) {
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
        return FluidTankBlockEntity.containedFluid(this.container, registries);
    }

    @Override
    public int getTankCapacity(int tank) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        return FluidTankBlockEntity.capacityOf(this.container, registries);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        return FluidTankBlockEntity.isFluidValid(this.container, stack, registries);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        // 模拟时不能动到真正的容器物品，只能在一件的副本上试
        ItemStack target = action.execute() ? this.container : this.container.copyWithCount(1);
        return FluidTankBlockEntity.insertIntoItem(target, resource, registries);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        ItemStack target = action.execute() ? this.container : this.container.copyWithCount(1);
        int drained = FluidTankBlockEntity.drainItem(target, resource, registries);
        return drained <= 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack content = this.getFluidInTank(0);
        return content.isEmpty() ? FluidStack.EMPTY : this.drain(content.copyWithAmount(maxDrain), action);
    }
}
