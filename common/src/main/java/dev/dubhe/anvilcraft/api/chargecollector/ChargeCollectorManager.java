package dev.dubhe.anvilcraft.api.chargecollector;

import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ChargeCollectorManager {
    private static final Map<BlockPos, ChargeCollectorBlockEntity> CHARGE_COLLECTOR_MAP = new HashMap<>();

    /**
     * 添加新的集电器
     */
    public static void addChargeCollector(ChargeCollectorBlockEntity blockEntity) {
        CHARGE_COLLECTOR_MAP.put(blockEntity.getBlockPos(), blockEntity);
    }

    /**
     * 删除集电器
     */
    public static void removeChargeCollector(ChargeCollectorBlockEntity blockEntity) {
        CHARGE_COLLECTOR_MAP.remove(blockEntity.getBlockPos());
    }

    /**
     * 获取最近的集电器的Collection集合(以从近至远排序)
     */
    public static Collection<Entry<Float, ChargeCollectorBlockEntity>> getNearestChargeCollect(BlockPos blockPos) {
        Map<Float, ChargeCollectorBlockEntity> distanceMap = new HashMap<>();
        for (Map.Entry<BlockPos, ChargeCollectorBlockEntity> entry : CHARGE_COLLECTOR_MAP.entrySet()) {
            float distance = Vector3f.distance(
                entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ(),
                blockPos.getX(), blockPos.getY(), blockPos.getZ());
            distanceMap.put(distance, entry.getValue());
        }
        return distanceMap.entrySet().stream()
            .sorted(Entry.comparingByKey()).collect(Collectors.toList());
    }

    /**
     * 判断是否能被集电器收集
     *
     * @param blockEntity 集电器方块实体
     * @param blockPos    电荷的位置
     * @return 是否能被集点器收集
     */
    public static boolean canCollect(ChargeCollectorBlockEntity blockEntity, BlockPos blockPos) {
        return blockEntity.getPos().getX() - 2 <= blockPos.getX()
            && blockEntity.getPos().getY() - 2 <= blockPos.getY()
            && blockEntity.getPos().getZ() - 2 <= blockPos.getZ()
            && blockEntity.getPos().getX() + 2 >= blockPos.getX()
            && blockEntity.getPos().getY() + 2 >= blockPos.getY()
            && blockEntity.getPos().getZ() + 2 >= blockPos.getZ();
    }

    public static void cleanMap() {
        CHARGE_COLLECTOR_MAP.clear();
    }
}
