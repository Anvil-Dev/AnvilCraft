package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.util.ExperienceMending;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidType;

public class ExpFluidCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable {
    public ExpFluidCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.EXP_FLUID);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!this.isEntityInsideContent(state, pos, entity)) return;
        if (entity instanceof ItemEntity item) {
            if (!this.isFull(state)) {
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

    @Override
    public ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        CauldronInteraction interaction = this.interactions.map().get(stack.getItem());
        if (interaction == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return interaction.interact(state, level, pos, player, hand, stack);
    }
}
