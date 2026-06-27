package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class HitBeeNestBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        double fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!state.hasBlockEntity()) return false;
        int honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < BeehiveBlock.MAX_HONEY_LEVELS) return false;
        BlockPos posBelowHive = pos.below();

        // 鱼缸支持：将蜂蜜灌入下方鱼缸等流体容器
        BlockEntity blockEntity = level.getBlockEntity(posBelowHive);
        if (blockEntity instanceof IFluidResourceHandlerHolder holder) {
            ResourceHandler<FluidResource> fluidHandler = holder.getFluidHandler();
            FluidResource honey = FluidResource.of(ModFluids.HONEY.get());
            int amount = FluidType.BUCKET_VOLUME / 4;
            try (Transaction transaction = Transaction.openRoot()) {
                int filled = fluidHandler.insert(honey, amount, transaction);
                if (filled < amount) return false;
                transaction.commit();
            }
            level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 2));
            return true;
        }

        // 炼药锅逻辑
        int filled = CauldronUtil.fill(level, posBelowHive, ModBlocks.HONEY_CAULDRON.get(), 1, true);
        if (filled <= 0) return false;
        CauldronUtil.fill(level, posBelowHive, ModBlocks.HONEY_CAULDRON.get(), 1, false);
        level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 2));
        return true;
    }
}
