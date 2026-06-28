package dev.dubhe.anvilcraft.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnvilUtil {
    public static void dropItems(List<ItemStack> items, Level level, Vec3 pos) {
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            int count = item.getCount();
            int maxStack = item.getMaxStackSize();
            for (; count >= maxStack; count -= maxStack) {
                ItemEntity entity = new ItemEntity(
                    level,
                    pos.x,
                    pos.y,
                    pos.z,
                    item.copyWithCount(maxStack),
                    0.0d,
                    0.0d,
                    0.0d
                );
                level.addFreshEntity(entity);
            }
            if (count <= 0) continue;
            ItemEntity entity = new ItemEntity(
                level,
                pos.x,
                pos.y,
                pos.z,
                item.copyWithCount(count),
                0.0d,
                0.0d,
                0.0d
            );
            entity.anvilcraft$setIsAdsorbable(false);
            level.addFreshEntity(entity);
        }
    }

    /// 探测盒的半边长（物品实体宽度的一半，0.125）。
    private static final double OUTLET_PROBE_HALF = 0.125;

    /// 薄壁厚度阈值。开口处虽有碰撞，但越过此深度后是空腔时，视为薄壁（如炼药锅壁、鱼缸壁），物品可以越过薄壁落入内部，因此不阻挡输出。
    private static final double OUTLET_WALL_MAX = 0.125;

    /**
     * 判断输出口开口处是否被前方方块的碰撞形状堵住。
     *
     * <p>在开口中心放一个物品大小（0.25³）的探测盒，与前方那一格方块的碰撞形状求交。
     * 相交即视为被堵。这样完整方块/台阶等会阻挡，而碰撞够不到开口的方块（灯笼、关闭的
     * 栅栏门等）以及无碰撞方块（告示牌、压力板、开启的栅栏门）都不会阻挡。
     *
     * <p>若开口处有碰撞，但越过薄壁后的内部是空腔（如炼药锅、鱼缸的内部），则视为薄壁，
     * 物品可以落入内部，仍允许输出。
     *
     * @param level         世界
     * @param frontPos      开口前方的方块坐标
     * @param openingCenter 开口中心的世界坐标（紧贴方块交界面）
     * @param direction     输出口朝向（开口指向前方方块的方向）
     * @return 被堵返回 true
     */
    public static boolean isOutletBlocked(Level level, BlockPos frontPos, Vec3 openingCenter, Direction direction) {
        BlockState frontState = level.getBlockState(frontPos);
        VoxelShape collisionShape = frontState.getCollisionShape(level, frontPos, CollisionContext.empty());
        if (collisionShape.isEmpty()) return false;
        // 把方块碰撞形状移动到世界坐标
        VoxelShape worldShape = collisionShape.move(frontPos.getX(), frontPos.getY(), frontPos.getZ());
        // 1. 开口处没有碰撞 -> 不阻挡（灯笼、关闭的栅栏门、告示牌等障碍够不到开口）
        if (!intersectsOutletProbe(worldShape, openingCenter)) return false;
        // 2. 开口处有碰撞，但越过薄壁后的内部是空腔 -> 视为薄壁（炼药锅壁/鱼缸壁），允许输出进内部
        Vec3 interiorCenter = openingCenter.add(
            direction.getStepX() * (OUTLET_WALL_MAX + OUTLET_PROBE_HALF),
            direction.getStepY() * (OUTLET_WALL_MAX + OUTLET_PROBE_HALF),
            direction.getStepZ() * (OUTLET_WALL_MAX + OUTLET_PROBE_HALF)
        );
        return intersectsOutletProbe(worldShape, interiorCenter);
        // 3. 内部仍是实心 -> 阻挡
    }

    private static boolean intersectsOutletProbe(VoxelShape worldShape, Vec3 center) {
        AABB probe = new AABB(
            center.x - OUTLET_PROBE_HALF,
            center.y - OUTLET_PROBE_HALF,
            center.z - OUTLET_PROBE_HALF,
            center.x + OUTLET_PROBE_HALF,
            center.y + OUTLET_PROBE_HALF,
            center.z + OUTLET_PROBE_HALF
        );
        return Shapes.joinIsNotEmpty(worldShape, Shapes.create(probe), BooleanOp.AND);
    }
}
