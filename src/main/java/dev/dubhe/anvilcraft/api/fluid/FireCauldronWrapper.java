package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.block.Layered4LevelCauldronBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 燃烧油锅与未点燃油锅共用流体，因此单独提供能力以保留燃烧状态。 */
public final class FireCauldronWrapper implements IFluidHandler {
    private static final int AMOUNT_PER_LEVEL = FluidType.BUCKET_VOLUME / 4;
    private final Level level;
    private final BlockPos pos;

    public FireCauldronWrapper(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        BlockState state = this.level.getBlockState(this.pos);
        return state.is(ModBlocks.FIRE_CAULDRON) || state.is(ModBlocks.OIL_CAULDRON)
            ? new FluidStack(ModFluids.OIL.get(), state.getValue(Layered4LevelCauldronBlock.LEVEL) * AMOUNT_PER_LEVEL)
            : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.is(ModFluids.OIL.get()) && stack.getComponents().isEmpty();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        BlockState state = this.level.getBlockState(this.pos);
        if (resource.isEmpty() || !this.isFluidValid(0, resource)
            || !(state.is(ModBlocks.FIRE_CAULDRON) || state.is(ModBlocks.OIL_CAULDRON) || state.is(Blocks.CAULDRON))) {
            return 0;
        }
        int current = this.getFluidInTank(0).getAmount();
        int inserted = Math.min(resource.getAmount() / AMOUNT_PER_LEVEL,
            (FluidType.BUCKET_VOLUME - current) / AMOUNT_PER_LEVEL) * AMOUNT_PER_LEVEL;
        if (inserted > 0 && action.execute()) {
            this.setAmount(state, current + inserted);
        }
        return inserted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !this.isFluidValid(0, resource)) {
            return FluidStack.EMPTY;
        }
        return this.drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int current = this.getFluidInTank(0).getAmount();
        int extracted = Math.min(maxDrain / AMOUNT_PER_LEVEL, current / AMOUNT_PER_LEVEL) * AMOUNT_PER_LEVEL;
        if (extracted > 0 && action.execute()) {
            this.setAmount(this.level.getBlockState(this.pos), current - extracted);
        }
        return new FluidStack(ModFluids.OIL.get(), extracted);
    }

    private void setAmount(BlockState state, int amount) {
        BlockState result = amount == 0 ? Blocks.CAULDRON.defaultBlockState()
            : (state.is(Blocks.CAULDRON) ? ModBlocks.OIL_CAULDRON.getDefaultState() : state)
                .setValue(Layered4LevelCauldronBlock.LEVEL, amount / AMOUNT_PER_LEVEL);
        this.level.setBlockAndUpdate(this.pos, result);
    }
}
