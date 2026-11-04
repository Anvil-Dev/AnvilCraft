package dev.dubhe.anvilcraft.block.cauldron;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.util.ExperienceMending;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;

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
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            if (!this.isFull(state) || !this.isEntityInsideContent(state, pos, entity)) {
                return;
            }
            ItemStack stack = item.getItem().copy();
            if (ExperienceMending.repair(stack, serverLevel, FluidType.BUCKET_VOLUME,
                amount -> level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3) ? FluidType.BUCKET_VOLUME : 0)) {
                item.setItem(stack);
            }
            return;
        }
        if (entity instanceof Player player) {
            if (!this.isFull(state)) return;
            player.giveExperiencePoints(AnvilCraft.CONFIG.world.expFluidXpPerBlock);
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
        }
    }
}
