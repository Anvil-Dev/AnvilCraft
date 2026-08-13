package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/// 附魔书视为只读的液态魔咒桶：有魔咒时返回携带该魔咒的液态魔咒，空书返回空流体。
public class EnchantedBookFluidHandler implements IFluidHandlerItem {
    private final ItemStack container;

    public EnchantedBookFluidHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        ItemEnchantments enchantments = container.getOrDefault(
            DataComponents.STORED_ENCHANTMENTS,
            ItemEnchantments.EMPTY
        );
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            ResourceKey<Enchantment> key = entry.getKey().getKey();
            if (key != null) {
                FluidStack fluid = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), FluidType.BUCKET_VOLUME);
                fluid.set(ModComponents.LIQUID_ENCHANTMENT, key);
                return fluid;
            }
        }
        return FluidStack.EMPTY;
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
