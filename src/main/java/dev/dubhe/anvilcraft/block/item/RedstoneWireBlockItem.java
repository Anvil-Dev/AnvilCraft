package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 放置红石导线，并支持用同一物品调整已有导线的附着面。 */
public class RedstoneWireBlockItem extends BlockItem {
    public RedstoneWireBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState oldState = level.getBlockState(pos);
        if (oldState.getBlock() instanceof RedstoneWireBlock wire) {
            // 同一格已经有导线时，把玩家对新表面的点击解释为改挂面，避免必须先拆除再放置。
            BlockState placementState = wire.getStateForPlacement(context);
            if (placementState != null
                && placementState.getValue(RedstoneWireBlock.ATTACHMENT)
                    != oldState.getValue(RedstoneWireBlock.ATTACHMENT)) {
                if (wire.reattach(level, pos, placementState)) {
                    // 使用 sidedSuccess 保持原版 BlockItem 的客户端手部反馈，同时由服务端决定最终状态。
                    return InteractionResult.SUCCESS;
                }
            }
        }
        // 不是有效改挂操作时仍走原版放置流程，保留方块碰撞、权限和物品消耗等通用检查。
        return super.place(context);
    }
}
