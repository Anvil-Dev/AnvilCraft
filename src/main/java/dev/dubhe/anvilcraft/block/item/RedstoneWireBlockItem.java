package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 放置红石导线，并支持用同一物品调整已有导线的附着面和局部端口。 */
public class RedstoneWireBlockItem extends BlockItem {
    public RedstoneWireBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placementPos = context.getClickedPos();
        BlockPos hitPos = context.replacingClickedOnBlock()
            ? placementPos
            : placementPos.relative(context.getClickedFace().getOpposite());
        BlockState hitState = level.getBlockState(hitPos);
        if (hitState.getBlock() instanceof RedstoneWireBlock wire) {
            // 直接命中薄导线时 placementPos 在它的上方或侧方，必须使用反推得到的真实命中格。
            return this.editWire(context, hitPos, hitState, wire, false, true);
        }

        BlockState placementState = level.getBlockState(placementPos);
        if (placementState.getBlock() instanceof RedstoneWireBlock wire) {
            // 点击导线下方或旁边的支撑面时，放置目标格本身就是已有导线。
            return this.editWire(context, placementPos, placementState, wire, true, false);
        }
        // 不是有效编辑操作时仍走原版放置流程，保留碰撞、权限和物品消耗等通用检查。
        return super.place(context);
    }

    private InteractionResult editWire(
        BlockPlaceContext context,
        BlockPos pos,
        BlockState oldState,
        RedstoneWireBlock wire,
        boolean allowReattach,
        boolean directHit
    ) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player != null && !player.mayBuild()) {
            return InteractionResult.FAIL;
        }
        if (allowReattach) {
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
        if (wire.editConnection(level, pos, oldState, context.getClickLocation(), directHit)) {
            // 端口编辑只修改稀疏网络覆盖数据，不执行 BlockItem 的放置和物品扣除流程。
            return Util.sidedSuccess(level);
        }
        return InteractionResult.FAIL;
    }
}
