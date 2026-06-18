package dev.dubhe.anvilcraft.block.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 限制方块放置间距的抽象 {@link BlockItem}。
 *
 * <p>
 * 当玩家尝试放置该方块时，会检查放置点周围指定半径（由 {@link #getIntervalsRadius()} 定义）内
 * 是否已存在同种方块。若已存在，则阻止放置；否则允许放置。
 *
 * <p>
 * 子类需实现 {@link #getIntervalsRadius()} 以定义有效的放置间距。
 *
 * @author burin
 * @see 1.6
 */
public abstract class PlacementIntervalsBlockItem extends BlockItem {
    public PlacementIntervalsBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public boolean doRangeNoOverlap() {
        return false;
    }

    /**
     * 获取放置间距半径。
     *
     * <p>
     * 以玩家点击位置为中心，该半径范围内的立方体区域内不允许存在另一个同种方块。
     *
     * @return 放置间距半径（方块数）
     */
    public abstract int getIntervalsRadius();

    /**
     * 判断是否可以在指定上下文中放置该方块。
     *
     * <p>
     * 除满足 {@link BlockItem#canPlace(BlockPlaceContext, BlockState)} 的条件外，
     * 还需确保放置点周围 {@link #getIntervalsRadius()} 范围内不存在同种方块。
     *
     * @param context 方块放置上下文，包含放置位置、玩家等信息
     * @param state   即将放置的方块状态
     * @return 如果可以放置返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean canPlace(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        Iterable<BlockPos> blockPoss = BlockPos.betweenClosed(
            clickedPos.offset(getIntervalsRadius(), getIntervalsRadius(), getIntervalsRadius()),
            clickedPos.offset(-getIntervalsRadius(), -getIntervalsRadius(), -getIntervalsRadius())
        );
        for (BlockPos blockPos : blockPoss) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.is(this.getBlock())) {
                if (level.isClientSide() && player instanceof LocalPlayer localPlayer) {
                    if (this.doRangeNoOverlap()) {
                        localPlayer.displayClientMessage(
                            Component.translatable("screen.anvilcraft.range_no_overlap")
                                .withStyle(ChatFormatting.RED),
                            true
                        );
                    } else {
                        localPlayer.displayClientMessage(
                            Component.translatable("screen.anvilcraft.range_overlap",
                                this.getIntervalsRadius() * 2 + 1).withStyle(ChatFormatting.RED),
                            true
                        );
                    }
                }
                return false;
            }
        }
        return super.canPlace(context, state);
    }
}
