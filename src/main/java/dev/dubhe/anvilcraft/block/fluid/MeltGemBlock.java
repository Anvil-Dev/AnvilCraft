package dev.dubhe.anvilcraft.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;

public class MeltGemBlock extends LiquidBlock {
    public MeltGemBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!FluidInteractionRegistry.canInteract(level, pos)) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
        }
    }
}
