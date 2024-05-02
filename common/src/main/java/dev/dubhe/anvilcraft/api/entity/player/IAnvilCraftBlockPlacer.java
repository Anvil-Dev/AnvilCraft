package dev.dubhe.anvilcraft.api.entity.player;

import dev.dubhe.anvilcraft.block.state.Orientation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
        Vec3 clickClickLocation = getPosFromOrientation(orientation);
        double x = clickClickLocation.x;
        double y = clickClickLocation.y;
        double z = clickClickLocation.z;
        getPlayer().teleportTo(0.5, -1, 0.5);
        getPlayer().lookAt(Anchor.EYES, new Vec3(x, 1 - y, z));
        BlockHitResult blockHitResult = new BlockHitResult(
            pos.getCenter().add(-0.5, -0.5, -0.5).add(x, 1 - y, z),
            orientation.getDirection().getOpposite(),
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
        Class<?> blockItemClass = blockItem.getClass();
        try {
            Method blockItemMethod = Arrays.stream(blockItemClass.getMethods()).toList().get(8);
            blockItemMethod.setAccessible(true);
            BlockState blockState = (BlockState) blockItemMethod.invoke(blockItem, blockPlaceContext);
            if ((blockState) == null) return InteractionResult.FAIL;
            level.setBlockAndUpdate(pos, blockState);
        } catch (InvocationTargetException | IllegalAccessException e) {
            Log.error(LogCategory.LOG, "Failed to get blockState " + e.getLocalizedMessage());
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    private Vec3 getPosFromOrientation(Orientation orientation) {
        return switch (orientation) {
            case NORTH_UP -> new Vec3(0.5, 0.5, 0.3);
            case SOUTH_UP -> new Vec3(0.5, 0.5, 0.7);
            case WEST_UP -> new Vec3(0.3, 0.5, 0.5);
            case EAST_UP -> new Vec3(0.7, 0.5, 0.5);
            case UP_NORTH -> new Vec3(0.5, 0.1, 0.7);
            case UP_SOUTH -> new Vec3(0.5, 0.1, 0.3);
            case UP_WEST -> new Vec3(0.7, 0.1, 0.5);
            case UP_EAST -> new Vec3(0.3, 0.1, 0.5);
            case DOWN_NORTH -> new Vec3(0.5, 0.9, 0.7);
            case DOWN_SOUTH -> new Vec3(0.5, 0.9, 0.3);
            case DOWN_WEST -> new Vec3(0.7, 0.9, 0.5);
            case DOWN_EAST -> new Vec3(0.3, 0.9, 0.5);
        };
    }
}
