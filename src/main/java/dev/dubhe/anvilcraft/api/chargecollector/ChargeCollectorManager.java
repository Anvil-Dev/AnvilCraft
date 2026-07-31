package dev.dubhe.anvilcraft.api.chargecollector;

import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.InfiniteCollectorBlockEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChargeCollectorManager {
    private static final Map<Level, ChargeCollectorManager> INSTANCES = new HashMap<>();
    private final Map<BlockPos, ChargeCollectorBlockEntity> chargeCollectors = new HashMap<>();
    private final Map<BlockPos, InfiniteCollectorBlockEntity> infiniteCollectors = new HashMap<>();

    @Getter
    private final Level level;

    public ChargeCollectorManager(Level level) {
        this.level = level;
    }

    /// 获取当前维度的ChargeCollectorManager
    public static ChargeCollectorManager getInstance(Level level) {
        if (!ChargeCollectorManager.INSTANCES.containsKey(level)) {
            ChargeCollectorManager.INSTANCES.put(level, new ChargeCollectorManager(level));
        }
        return ChargeCollectorManager.INSTANCES.get(level);
    }

    /// 充电
    ///
    /// @param chargeNum 充电量
    /// @param level     维度
    /// @param blockPos  充电的位置
    public static void charge(double chargeNum, Level level, BlockPos blockPos) {
        ChargeCollectorManager instance = ChargeCollectorManager.getInstance(level);
        instance.charge(chargeNum, blockPos);
    }

    /// 充电
    ///
    /// @param chargeNum 充电量
    /// @param blockPos  充电的位置
    public void charge(double chargeNum, BlockPos blockPos) {
        Collection<Entry> chargeCollectorCollection = this.getNearestChargeCollect(blockPos);
        double surplus = chargeNum;
        for (Entry entry : chargeCollectorCollection) {
            if (entry.isInfinite()) {
                InfiniteCollectorBlockEntity ic = entry.infiniteCollector();
                if (ic == null || !this.canCollect(ic, blockPos)) continue;
                surplus = ic.incomingCharge(surplus, blockPos);
            } else {
                ChargeCollectorBlockEntity cc = entry.chargeCollector();
                if (cc == null || !this.canCollect(cc, blockPos)) continue;
                surplus = cc.incomingCharge(surplus, blockPos);
            }
            if (surplus == 0) return;
        }
    }

    /// 添加新的集电器
    public void addChargeCollector(ChargeCollectorBlockEntity blockEntity) {
        this.chargeCollectors.put(blockEntity.getBlockPos(), blockEntity);
    }

    /// 删除集电器
    public void removeChargeCollector(ChargeCollectorBlockEntity blockEntity) {
        this.chargeCollectors.remove(blockEntity.getBlockPos());
    }

    /// 添加新的无限收集器
    public void addInfiniteCollector(InfiniteCollectorBlockEntity blockEntity) {
        this.infiniteCollectors.put(blockEntity.getBlockPos(), blockEntity);
    }

    /// 删除无限收集器
    public void removeInfiniteCollector(InfiniteCollectorBlockEntity blockEntity) {
        this.infiniteCollectors.remove(blockEntity.getBlockPos());
    }

    /// 获取最近的收集器的List集合(以从近至远排序)
    public List<Entry> getNearestChargeCollect(BlockPos blockPos) {
        List<Entry> distanceList = new ArrayList<>();
        for (Map.Entry<BlockPos, ChargeCollectorBlockEntity> entry : this.chargeCollectors.entrySet()) {
            double distance = Vector3f.distance(
                entry.getKey().getX(),
                entry.getKey().getY(),
                entry.getKey().getZ(),
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ()
            );
            distanceList.add(new Entry(distance, entry.getValue(), null));
        }
        for (Map.Entry<BlockPos, InfiniteCollectorBlockEntity> entry : this.infiniteCollectors.entrySet()) {
            double distance = Vector3f.distance(
                entry.getKey().getX(),
                entry.getKey().getY(),
                entry.getKey().getZ(),
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ()
            );
            distanceList.add(new Entry(distance, null, entry.getValue()));
        }
        return distanceList.stream()
            .sorted(Comparator.comparing(Entry::distance))
            .collect(Collectors.toList());
    }

    /// 判断是否能被集电器收集
    ///
    /// @param blockEntity 集电器方块实体
    /// @param blockPos    电荷的位置
    /// @return 是否能被集点器收集
    public boolean canCollect(ChargeCollectorBlockEntity blockEntity, BlockPos blockPos) {
        return blockEntity.getPos().getX() - 2 <= blockPos.getX()
            && blockEntity.getPos().getY() - 2 <= blockPos.getY()
            && blockEntity.getPos().getZ() - 2 <= blockPos.getZ()
            && blockEntity.getPos().getX() + 2 >= blockPos.getX()
            && blockEntity.getPos().getY() + 2 >= blockPos.getY()
            && blockEntity.getPos().getZ() + 2 >= blockPos.getZ();
    }

    /// 判断是否能被无限收集器收集
    ///
    /// @param blockEntity 无限收集器方块实体
    /// @param blockPos    电荷的位置
    /// @return 是否能被收集
    public boolean canCollect(InfiniteCollectorBlockEntity blockEntity, BlockPos blockPos) {
        int range = blockEntity.getRange();
        return blockEntity.getPos().getX() - range <= blockPos.getX()
            && blockEntity.getPos().getY() - range <= blockPos.getY()
            && blockEntity.getPos().getZ() - range <= blockPos.getZ()
            && blockEntity.getPos().getX() + range >= blockPos.getX()
            && blockEntity.getPos().getY() + range >= blockPos.getY()
            && blockEntity.getPos().getZ() + range >= blockPos.getZ();
    }

    public record Entry(
        double distance,
        @Nullable ChargeCollectorBlockEntity chargeCollector,
        @Nullable InfiniteCollectorBlockEntity infiniteCollector
    ) {

        public boolean isInfinite() {
            return this.infiniteCollector != null;
        }
    }
}
