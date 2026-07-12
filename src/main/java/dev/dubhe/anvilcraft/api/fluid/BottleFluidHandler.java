package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Optional;

/**
 * Fluid handler for bottle-like items. Each bottle transfers 250 mB.
 */
public class BottleFluidHandler implements IFluidHandlerItem {
    private static final int FLUID_PER_BOTTLE = 250;

    protected ItemStack container;

    public BottleFluidHandler(ItemStack container) {
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
        if (container.is(Items.HONEY_BOTTLE)) {
            return new FluidStack(ModFluids.HONEY, FLUID_PER_BOTTLE);
        }
        if (container.is(Items.POTION) && isWaterPotion(container)) {
            return new FluidStack(Fluids.WATER, FLUID_PER_BOTTLE);
        }
        if (container.is(Items.EXPERIENCE_BOTTLE)) {
            return new FluidStack(ModFluids.EXP_FLUID, FLUID_PER_BOTTLE);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return FLUID_PER_BOTTLE;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid() instanceof HoneyFluid
            || stack.is(Fluids.WATER)
            || stack.is(ModFluids.EXP_FLUID);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.getAmount() < FLUID_PER_BOTTLE) return 0;
        if (!container.is(Items.GLASS_BOTTLE)) return 0;
        ItemStack filled = getFilledBottle(resource);
        if (filled.isEmpty()) return 0;

        if (action.execute()) {
            container = filled;
        }
        return FLUID_PER_BOTTLE;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain < FLUID_PER_BOTTLE) return FluidStack.EMPTY;
        FluidStack result = getFluidInTank(0);
        if (result.isEmpty()) return FluidStack.EMPTY;

        if (action.execute()) {
            container = new ItemStack(Items.GLASS_BOTTLE);
        }
        return result;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.getAmount() < FLUID_PER_BOTTLE) return FluidStack.EMPTY;
        FluidStack result = getFluidInTank(0);
        if (result.isEmpty()) return FluidStack.EMPTY;
        if (!result.is(resource.getFluid())) return FluidStack.EMPTY;

        if (action.execute()) {
            container = new ItemStack(Items.GLASS_BOTTLE);
        }
        return result;
    }

    private static ItemStack getFilledBottle(FluidStack resource) {
        if (resource.getFluid() instanceof HoneyFluid) {
            return new ItemStack(Items.HONEY_BOTTLE);
        }
        if (resource.is(Fluids.WATER)) {
            return PotionContents.createItemStack(Items.POTION, Potions.WATER);
        }
        if (resource.is(ModFluids.EXP_FLUID)) {
            return new ItemStack(Items.EXPERIENCE_BOTTLE);
        }
        return ItemStack.EMPTY;
    }

    private static boolean isWaterPotion(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        Optional<Holder<Potion>> potion = contents.potion();
        return potion.isPresent() && potion.get() == Potions.WATER;
    }
}
