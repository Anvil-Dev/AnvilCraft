package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class ExpFluidBlock extends LiquidBlock {

    public ExpFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effectApplier,
        boolean isPrecise
    ) {
        if (level.isClientSide()) return;
        if (!level.getFluidState(pos).isSource()) return;
        if (entity instanceof Player player) {
            player.giveExperiencePoints(AnvilCraft.CONFIG.world.expFluidXpPerBlock);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
