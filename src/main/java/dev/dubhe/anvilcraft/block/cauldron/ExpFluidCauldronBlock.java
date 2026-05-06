package dev.dubhe.anvilcraft.block.cauldron;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.ExpFluidBlock;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ExpFluidCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable {
    public ExpFluidCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.EXP_FLUID);
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
        if (entity instanceof Player player) {
            if (!this.isFull(state)) return;
            player.giveExperiencePoints(ExpFluidBlock.XP_POINTS);
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
        }
    }
}
