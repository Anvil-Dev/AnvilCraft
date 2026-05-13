package dev.dubhe.anvilcraft.item.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public class EndDustBlockItem extends BlockItem {
    public EndDustBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(
        Level level, Player player, InteractionHand usedHand) {
        BlockPos blockPos = level.clip(new ClipContext(
                player.getEyePosition(1F),
                player.getEyePosition(1F).add(player.getViewVector(1F).scale(2.5)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player))
            .getBlockPos();
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!(level.getBlockState(blockPos).is(BlockTags.REPLACEABLE))) return InteractionResult.FAIL;
        BlockHitResult blockHitResult =
            new BlockHitResult(blockPos.getCenter(), player.getDirection(), blockPos, false);
        BlockPlaceContext blockPlaceContext = new BlockPlaceContext(level, player, usedHand, itemStack, blockHitResult);
        if (!this.canPlace(blockPlaceContext, this.getBlock().defaultBlockState())) return InteractionResult.FAIL;
        if (this.place(blockPlaceContext) == InteractionResult.FAIL) return InteractionResult.FAIL;
        return InteractionResult.SUCCESS;
    }
}
