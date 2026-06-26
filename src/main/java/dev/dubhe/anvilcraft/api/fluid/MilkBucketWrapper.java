package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

/**
 * 牛奶桶流体处理器包装器
 * 使牛奶桶可以跟储罐等进行流体交互
 */
public class MilkBucketWrapper extends FluidBucketWrapper {
    public MilkBucketWrapper(ItemStack container) {
        super(container);
    }

    @Override
    public FluidStack getFluid() {
        if (this.container.is(Items.MILK_BUCKET)) {
            return new FluidStack(ModFluids.MILK, FluidType.BUCKET_VOLUME);
        }
        return super.getFluid();
    }
}
