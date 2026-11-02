package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 在牛奶的全局锅映射由其它模组占用时，保留四层牛奶锅的流体交互。 */
public class MilkCauldronWrapper implements IFluidHandler {
    private static final int AMOUNT_PER_LEVEL = FluidType.BUCKET_VOLUME / 4;
    private final Level level;
    private final BlockPos pos;

    public MilkCauldronWrapper(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        var state = this.level.getBlockState(this.pos);
        return state.is(ModBlocks.MILK_CAULDRON)
            ? new FluidStack(NeoForgeMod.MILK.get(), CauldronUtil.currentLevel(state) * AMOUNT_PER_LEVEL)
            : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.is(NeoForgeMod.MILK.get());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !this.isFluidValid(0, resource)) {
            return 0;
        }
        return CauldronUtil.fill(this.level, this.pos, ModBlocks.MILK_CAULDRON.get(),
            resource.getAmount() / AMOUNT_PER_LEVEL, action.simulate()) * AMOUNT_PER_LEVEL;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !this.isFluidValid(0, resource) || !resource.getComponents().isEmpty()) {
            return FluidStack.EMPTY;
        }
        return this.drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int drained = CauldronUtil.drain(this.level, this.pos, ModBlocks.MILK_CAULDRON.get(),
            maxDrain / AMOUNT_PER_LEVEL, action.simulate());
        return new FluidStack(NeoForgeMod.MILK.get(), drained * AMOUNT_PER_LEVEL);
    }
}
