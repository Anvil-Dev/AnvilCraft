package dev.dubhe.anvilcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class EndDustBlockItem extends BlockItem {
    public EndDustBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        BlockPos blockPos = level.clip(new ClipContext(
                player.getEyePosition(1f),
                player.getEyePosition(1f).add(player.getViewVector(1f).scale(2.5)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player))
            .getBlockPos();
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!(level.getBlockState(blockPos).is(BlockTags.REPLACEABLE))) return InteractionResultHolder.fail(itemStack);
        BlockHitResult blockHitResult =
            new BlockHitResult(blockPos.getCenter(), player.getDirection(), blockPos, false);
        BlockPlaceContext blockPlaceContext = new BlockPlaceContext(level, player, usedHand, itemStack, blockHitResult);
        if (!this.canPlace(blockPlaceContext, this.getBlock().defaultBlockState()))
            return InteractionResultHolder.fail(itemStack);
        if (this.place(blockPlaceContext) == InteractionResult.FAIL) return InteractionResultHolder.fail(itemStack);
        return InteractionResultHolder.success(itemStack);
    }
}
