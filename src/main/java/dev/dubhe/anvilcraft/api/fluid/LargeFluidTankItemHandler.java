package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/// 大型流体储罐物品的 {@link IFluidHandlerItem} 实现。
///
/// <p>槽位数与每槽容量都由物品内的储罐数据决定，因此多流体也能按槽位暴露给通用逻辑。</p>
public class LargeFluidTankItemHandler implements IFluidHandlerItem {
    private final ItemStack container;

    public LargeFluidTankItemHandler(ItemStack container) {
        this.container = container.copyWithCount(1);
    }

    @Override
    public ItemStack getContainer() {
        return this.container;
    }

    @Override
    public int getTanks() {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        return Math.max(1, LargeFluidTankBlockEntity.tankCountOf(this.container, registries));
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        return LargeFluidTankBlockEntity.fluidInTank(this.container, tank, registries);
    }

    @Override
    public int getTankCapacity(int tank) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        return LargeFluidTankBlockEntity.tankCapacityOf(this.container, tank, registries);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank >= 0 && tank < this.getTanks() && !stack.isEmpty();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        // 模拟时不能动到真正的容器物品，只能在一件的副本上试
        ItemStack target = action.execute() ? this.container : this.container.copyWithCount(1);
        return LargeFluidTankBlockEntity.insertIntoItem(target, resource, registries);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        ItemStack target = action.execute() ? this.container : this.container.copyWithCount(1);
        int drained = LargeFluidTankBlockEntity.drainItem(target, resource, registries);
        return drained <= 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (int tank = 0; tank < this.getTanks(); tank++) {
            FluidStack content = this.getFluidInTank(tank);
            if (!content.isEmpty()) {
                return this.drain(content.copyWithAmount(maxDrain), action);
            }
        }
        return FluidStack.EMPTY;
    }
}
