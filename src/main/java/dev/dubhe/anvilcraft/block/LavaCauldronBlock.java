package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class LavaCauldronBlock extends LayeredCauldronBlock {
    public LavaCauldronBlock(Properties properties) {
        super(properties, p -> false, CauldronInteraction.LAVA);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (this.isEntityInsideContent(state, pos, entity)) {
            entity.lavaHurt();
        }
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, @NotNull Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        if (state.getValue(LEVEL) == 3) {
            level.setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
        }
    }
}
