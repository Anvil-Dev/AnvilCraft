package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public class PowderSnowWrapper extends FluidBucketWrapper {
    public PowderSnowWrapper(ItemStack container) {
        super(container);
    }

    @Override
    public FluidStack getFluid() {
        if (this.container.is(Items.POWDER_SNOW_BUCKET)) return new FluidStack(ModFluids.POWDER_SNOW, FluidType.BUCKET_VOLUME);
        return super.getFluid();
    }
}
