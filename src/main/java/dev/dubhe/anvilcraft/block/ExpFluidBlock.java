package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.ExperienceMending;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

public class ExpFluidBlock extends LiquidBlock {

    public ExpFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!level.getFluidState(pos).isSource()) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            if (item.getBoundingBox().minY >= pos.getY() + level.getFluidState(pos).getHeight(level, pos)) {
                return;
            }
            ItemStack stack = item.getItem().copy();
            if (ExperienceMending.repair(stack, serverLevel, FluidType.BUCKET_VOLUME,
                amount -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3) ? FluidType.BUCKET_VOLUME : 0)) {
                item.setItem(stack);
            }
            return;
        }
        if (entity instanceof Player player) {
            player.giveExperiencePoints(AnvilCraft.CONFIG.world.expFluidXpPerBlock);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
