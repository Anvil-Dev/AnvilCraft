package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class HitBeeNestBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos pos,
        BlockState state,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!state.hasBlockEntity()) return false;
        int honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < BeehiveBlock.MAX_HONEY_LEVELS) return false;
        BlockPos posBelowHive = pos.below();

        // 鱼缸支持
        if (level.getBlockEntity(posBelowHive) instanceof IFluidHandlerHolder holder) {
            FluidStack honey = new FluidStack(ModFluids.HONEY, FluidType.BUCKET_VOLUME / 4);
            int filled = holder.getFluidHandler().fill(honey, IFluidHandler.FluidAction.SIMULATE);
            if (filled >= honey.getAmount()) {
                holder.getFluidHandler().fill(honey, IFluidHandler.FluidAction.EXECUTE);
                level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 2));
                return true;
            }
            return false;
        }

        // 炼药锅逻辑
        int filled = CauldronUtil.fill(level, posBelowHive, ModBlocks.HONEY_CAULDRON.get(), 1, true);
        if (filled <= 0) return false;
        CauldronUtil.fill(level, posBelowHive, ModBlocks.HONEY_CAULDRON.get(), 1, false);
        level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 2));
        return true;
    }
}
