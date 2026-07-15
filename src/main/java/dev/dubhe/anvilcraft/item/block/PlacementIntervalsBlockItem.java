package dev.dubhe.anvilcraft.item.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public abstract class PlacementIntervalsBlockItem extends BlockItem {
    public PlacementIntervalsBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public boolean doRangeNoOverlap() {
        return false;
    }

    protected List<Block> getIntervalBlocks() {
        return List.of(this.getBlock());
    }

    public abstract int getIntervalsRadius();

    protected int getIntervalsRadius(BlockState state) {
        return this.getIntervalsRadius();
    }

    protected int getMaxIntervalsRadius() {
        return this.getIntervalsRadius();
    }

    protected boolean isIntervalBlock(BlockState state) {
        return this.getIntervalBlocks().stream().anyMatch(state::is);
    }

    @Override
    public boolean canPlace(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        int maxRadius = this.getMaxIntervalsRadius();
        Iterable<BlockPos> positions = BlockPos.betweenClosed(
            clickedPos.offset(maxRadius, maxRadius, maxRadius),
            clickedPos.offset(-maxRadius, -maxRadius, -maxRadius)
        );
        for (BlockPos blockPos : positions) {
            BlockState nearbyState = level.getBlockState(blockPos);
            int distance = Math.max(
                Math.max(Math.abs(blockPos.getX() - clickedPos.getX()), Math.abs(blockPos.getY() - clickedPos.getY())),
                Math.abs(blockPos.getZ() - clickedPos.getZ())
            );
            if (!this.isIntervalBlock(nearbyState) || distance > this.getIntervalsRadius(nearbyState)) continue;
            if (level.isClientSide() && player != null) {
                MutableComponent message = this.doRangeNoOverlap()
                                           ? Component.translatable("screen.anvilcraft.range_no_overlap")
                                           : Component.translatable(
                                               "screen.anvilcraft.range_overlap",
                                               this.getIntervalsRadius() * 2 + 1
                                           );
                player.sendOverlayMessage(message.withStyle(ChatFormatting.RED));
            }
            return false;
        }
        return super.canPlace(context, state);
    }
}
