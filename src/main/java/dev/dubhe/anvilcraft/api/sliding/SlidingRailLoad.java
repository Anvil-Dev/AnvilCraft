package dev.dubhe.anvilcraft.api.sliding;

import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 检测滑轨上的滑动载荷。在服务器 tick 中调用，查询不得改变世界状态。
 */
public final class SlidingRailLoad {
    private static final List<OccupantCheck> EXTRA = new CopyOnWriteArrayList<>();

    private SlidingRailLoad() {
    }

    public static void registerOccupant(OccupantCheck check) {
        EXTRA.add(check);
    }

    public static boolean hasBlockLoad(ServerLevel level, BlockPos railPos) {
        AABB above = new AABB(railPos.above());
        if (!level.getEntitiesOfClass(SlidingBlockEntity.class, above).isEmpty()) {
            return true;
        }
        for (OccupantCheck check : EXTRA) {
            if (check.test(level, railPos, above)) return true;
        }
        return false;
    }

    public static boolean hasItemLoad(ServerLevel level, BlockPos railPos) {
        return !level.getEntitiesOfClass(ItemEntity.class, new AABB(railPos)).isEmpty();
    }

    @FunctionalInterface
    public interface OccupantCheck {
        boolean test(ServerLevel level, BlockPos railPos, AABB above);
    }
}
