package dev.dubhe.anvilcraft.block.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseCauldronBlock extends AbstractCauldronBlock {
    public BaseCauldronBlock(Properties properties) {
        this(properties, new CauldronInteraction.Dispatcher());
    }

    public BaseCauldronBlock(Properties properties, CauldronInteraction.Dispatcher dispatcher) {
        super(properties, dispatcher);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return new ItemStack(Items.CAULDRON);
    }
}
