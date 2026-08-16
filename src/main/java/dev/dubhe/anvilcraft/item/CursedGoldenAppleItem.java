package dev.dubhe.anvilcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class CursedGoldenAppleItem extends Item {
    private static final int SEARCH_RADIUS = 16;

    public CursedGoldenAppleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (level instanceof ServerLevel serverLevel && livingEntity instanceof ServerPlayer player) {
            teleport(player, serverLevel);
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

    private static void teleport(ServerPlayer player, ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            ServerLevel nether = level.getServer().getLevel(Level.NETHER);
            if (nether != null) {
                BlockPos pos = new BlockPos(player.getBlockX() / 8, player.getBlockY(), player.getBlockZ() / 8);
                teleportToSafe(nether, player, pos);
            }
        } else if (level.dimension() == Level.NETHER) {
            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos pos = new BlockPos(player.getBlockX() * 8, player.getBlockY(), player.getBlockZ() * 8);
                teleportToSafe(overworld, player, pos);
            }
        } else if (level.dimension() == Level.END) {
            player.changeDimension(player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.PLAY_PORTAL_SOUND));
        }
    }

    private static void teleportToSafe(ServerLevel target, ServerPlayer player, BlockPos origin) {
        BlockPos safe = findSafeLandingPos(target, origin);
        if (safe == null) {
            safe = fallbackLandingPos(target, origin);
        }
        Vec3 pos = Vec3.atBottomCenterOf(safe);
        player.teleportTo(target, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
        playPortalSound(player);
    }

    private static void playPortalSound(ServerPlayer player) {
        player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
    }

    /**
     * 参考 PortalForcer.createPortal 的选址逻辑：在目标坐标附近螺旋搜索，沿高度图向下
     * 找到底部实心、上方有足够空间的落脚点。
     */
    @Nullable
    private static BlockPos findSafeLandingPos(ServerLevel level, BlockPos origin) {
        WorldBorder worldBorder = level.getWorldBorder();
        int maxPlaceableY = Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.getLogicalHeight() - 1);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (BlockPos.MutableBlockPos column : BlockPos.spiralAround(origin, SEARCH_RADIUS, Direction.EAST, Direction.SOUTH)) {
            if (!worldBorder.isWithinBounds(column)) continue;
            int height = Math.min(maxPlaceableY, level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()));
            for (int y = height; y >= level.getMinBuildHeight(); y--) {
                column.setY(y);
                if (!canTeleportReplaceBlock(level, column)) continue;
                int firstEmptyY = y;
                while (y > level.getMinBuildHeight() && canTeleportReplaceBlock(level, column.move(Direction.DOWN))) {
                    y--;
                }
                if (y + 4 > maxPlaceableY) continue;
                int airHeight = firstEmptyY - y;
                if (airHeight > 0 && airHeight < 3) continue;
                column.setY(y);
                if (canHostPlayer(level, column, cursor)) {
                    return column.immutable();
                }
            }
        }
        return null;
    }

    private static boolean canTeleportReplaceBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    private static boolean canHostPlayer(ServerLevel level, BlockPos origin, BlockPos.MutableBlockPos cursor) {
        for (int width = -1; width < 3; width++) {
            for (int height = -1; height < 4; height++) {
                cursor.setWithOffset(origin, width, height, 0);
                if (height < 0) {
                    if (!level.getBlockState(cursor).isSolid()) return false;
                } else if (!canTeleportReplaceBlock(level, cursor)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BlockPos fallbackLandingPos(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = level.getWorldBorder().clampToBounds(origin).mutable();
        int maxPlaceableY = Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.getLogicalHeight() - 1);
        int height = Math.min(maxPlaceableY, level.getHeight(Heightmap.Types.MOTION_BLOCKING, cursor.getX(), cursor.getZ()));
        for (int y = height; y >= level.getMinBuildHeight(); y--) {
            cursor.setY(y);
            if (level.getBlockState(cursor).isSolid()
                && level.isEmptyBlock(cursor.above())
                && level.isEmptyBlock(cursor.above(2))) {
                return cursor.above().immutable();
            }
        }
        return origin.immutable();
    }
}
