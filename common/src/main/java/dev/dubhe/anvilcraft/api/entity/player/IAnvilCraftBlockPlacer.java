package dev.dubhe.anvilcraft.api.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 假人方块放置器
 */
public interface IAnvilCraftBlockPlacer {
    ServerPlayer getPlayer();

    /**
     * 放置方块
     *
     * @param level 放置世界
     * @param pos 放置位置
     * @param direction 放置方向
     * @param blockItem 放置方块物品
     * @return 放置结果
     */
    default InteractionResult placeBlock(Level level, BlockPos pos, Direction direction,
        BlockItem blockItem) {
        BlockHitResult blockHitResult = new BlockHitResult(
            pos.below().relative(direction.getOpposite()).getCenter(),
            direction,
            pos,
            false
          );
        BlockPlaceContext blockPlaceContext =
              new BlockPlaceContext(level,
                  getPlayer(),
                  getPlayer().getUsedItemHand(),
                  new ItemStack(blockItem),
                  blockHitResult
              );
        return blockItem.place(blockPlaceContext);
    }
}
