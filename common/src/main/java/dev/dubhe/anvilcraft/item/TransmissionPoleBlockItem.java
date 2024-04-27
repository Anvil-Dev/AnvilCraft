package dev.dubhe.anvilcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class TransmissionPoleBlockItem extends BlockItem {

    public TransmissionPoleBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        InteractionResult result = super.place(context);
        if (result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME) {
            level.playSound(null,
                context.getClickedPos(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                1f,
                1f);
        }
        return result;
    }

    @Override
    public boolean canPlace(BlockPlaceContext context, @NotNull BlockState state) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        return level.getBlockState(blockPos.above()).is(BlockTags.REPLACEABLE)
            && level.getBlockState(blockPos.above(2)).is(BlockTags.REPLACEABLE);
    }
}
