package dev.dubhe.anvilcraft.block.better;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public abstract class BetterAbstractCauldronBlock extends AbstractCauldronBlock {
    public BetterAbstractCauldronBlock(
            Properties pProperties, CauldronInteraction.InteractionMap pInteractions) {
        super(pProperties, pInteractions);
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(Items.CAULDRON);
    }
}
