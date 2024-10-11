package dev.dubhe.anvilcraft.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public abstract class Utils {

    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTH
    };

    private Utils() {
    }

    /**
     * @return 模组是否加载
     */
    @ExpectPlatform
    public static boolean isLoaded(String modid) {
        throw new AssertionError();
    }

    /**
     * 水平方向
     */
    public static void acceptHorizontalDirections(BlockPos blockPos, Consumer<BlockPos> blockPosConsumer) {
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            blockPosConsumer.accept(blockPos.relative(direction));
        }
    }

    /**
     * 是否可以交互
     */
    public static boolean canInteract(Player player, BlockPos pos, double distance) {
        return player.distanceToSqr(
            (double) pos.getX() + 0.5,
            (double) pos.getY() + 0.5,
            (double) pos.getZ() + 0.5
        ) <= distance * distance;
    }
}
