package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.block.cauldron.ObsidianCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MengerSpongeBlockItem extends BlockItem {
    public MengerSpongeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractCauldronBlock abstractCauldronBlock) {
            if (!(abstractCauldronBlock instanceof ObsidianCauldronBlock)) {
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }
}
