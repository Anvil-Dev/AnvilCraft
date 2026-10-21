package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.util.LiquidEnchantmentUtil;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/// 书/附魔书视为只读的液态魔咒桶：有魔咒时返回携带该魔咒的液态魔咒，无魔咒时返回空白液态魔咒。
public class EnchantedBookFluidHandler implements IFluidHandlerItem {
    private final ItemStack container;

    public EnchantedBookFluidHandler(ItemStack container) {
        this.container = container;
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
        return LiquidEnchantmentUtil.fromBook(this.container).copyWithAmount(FluidType.BUCKET_VOLUME);
    }

    @Override
    public int getTankCapacity(int tank) {
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    /// 只读桶：不支持填充或抽取，仅用于读取标记。
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
