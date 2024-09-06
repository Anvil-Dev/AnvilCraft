package dev.dubhe.anvilcraft.api.chargecollector;

import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public class ChargeCollectorManager {
    private static final Map<Level, ChargeCollectorManager> INSTANCES = new HashMap<>();
    private final Map<BlockPos, ChargeCollectorBlockEntity> chargeCollectors = new HashMap<>();
    @Getter
    private final Level level;

    public ChargeCollectorManager(Level level) {
        this.level = level;
    }

    /**
     * 获取当前维度的ChargeCollectorManager
     */
    public static ChargeCollectorManager getInstance(Level level) {
        if (!INSTANCES.containsKey(level)) {
            INSTANCES.put(level, new ChargeCollectorManager(level));
        }
        return INSTANCES.get(level);
    }

    /**
     * 添加新的集电器
     */
    public void addChargeCollector(ChargeCollectorBlockEntity blockEntity) {
        chargeCollectors.put(blockEntity.getBlockPos(), blockEntity);
    }

    /**
     * 删除集电器
     */
    public void removeChargeCollector(ChargeCollectorBlockEntity blockEntity) {
        chargeCollectors.remove(blockEntity.getBlockPos());
    }

    /**
     * 获取最近的集电器的Collection集合(以从近至远排序)
     */
    public Collection<Entry> getNearestChargeCollect(BlockPos blockPos) {
        List<Entry> distanceList = new ArrayList<>();
        for (Map.Entry<BlockPos, ChargeCollectorBlockEntity> entry : chargeCollectors.entrySet()) {
            double distance = Vector3f.distance(
                    entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ(),
                    blockPos.getX(), blockPos.getY(), blockPos.getZ());
            distanceList.add(new Entry(distance, entry.getValue()));
        }
        return distanceList.stream()
                .sorted(Comparator.comparing(Entry::getDistance)).collect(Collectors.toList());
    }

    /**
     * 判断是否能被集电器收集
     *
     * @param blockEntity 集电器方块实体
     * @param blockPos    电荷的位置
     * @return 是否能被集点器收集
     */
    public boolean canCollect(ChargeCollectorBlockEntity blockEntity, BlockPos blockPos) {
        return blockEntity.getPos().getX() - 2 <= blockPos.getX()
                && blockEntity.getPos().getY() - 2 <= blockPos.getY()
                && blockEntity.getPos().getZ() - 2 <= blockPos.getZ()
                && blockEntity.getPos().getX() + 2 >= blockPos.getX()
                && blockEntity.getPos().getY() + 2 >= blockPos.getY()
                && blockEntity.getPos().getZ() + 2 >= blockPos.getZ();
    }

    @Getter
    public static class Entry {
        public final double distance;
        public final ChargeCollectorBlockEntity blockEntity;

        public Entry(Double distance, ChargeCollectorBlockEntity blockEntity) {
            this.distance = distance;
            this.blockEntity = blockEntity;
        }
    }
}
