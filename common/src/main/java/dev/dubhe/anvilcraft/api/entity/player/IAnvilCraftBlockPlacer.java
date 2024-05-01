package dev.dubhe.anvilcraft.api.entity.player;

import dev.dubhe.anvilcraft.block.state.Orientation;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
     * @param orientation 放置方向
     * @param blockItem 放置方块物品
     * @return 放置结果
     */
    default InteractionResult placeBlock(Level level, BlockPos pos, Orientation orientation,
        BlockItem blockItem) {
        getPlayer().lookAt(Anchor.EYES, getPosFromOrientation(orientation));
        BlockHitResult blockHitResult = new BlockHitResult(
            pos.below().relative(orientation.getDirection().getOpposite()).getCenter(),
            orientation.getDirection(),
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

    private Vec3 getPosFromOrientation(Orientation orientation) {
        return switch (orientation) {
            case NORTH_UP -> new Vec3(0.5, 0, 0.3);
            case SOUTH_UP -> new Vec3(0.5, 0, 0.7);
            case WEST_UP -> new Vec3(0.3, 0, 0.5);
            case EAST_UP -> new Vec3(0.7, 0, 0.5);
            case UP_NORTH -> new Vec3(0.5, 1.25, 0.7);
            case UP_SOUTH -> new Vec3(0.5, 1.25, 0.3);
            case UP_WEST -> new Vec3(0.7, 1.25, 0.5);
            case UP_EAST -> new Vec3(0.3, 1.25, 0.5);
            case DOWN_NORTH -> new Vec3(0.5, -0.25, 0.7);
            case DOWN_SOUTH -> new Vec3(0.5, -0.25, 0.3);
            case DOWN_WEST -> new Vec3(0.7, -0.25, 0.5);
            case DOWN_EAST -> new Vec3(0.3, -0.25, 0.5);
        };
    }
}
