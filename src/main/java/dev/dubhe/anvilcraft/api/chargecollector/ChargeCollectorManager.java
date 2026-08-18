package dev.dubhe.anvilcraft.api.chargecollector;

import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.InfiniteCollectorBlockEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public class ChargeCollectorManager {
    private static final Map<Level, ChargeCollectorManager> INSTANCES = new ConcurrentHashMap<>();
    private static final Comparator<Entry> ENTRY_COMPARATOR = Comparator.comparingDouble(Entry::getDistance)
        .thenComparing(Entry::isInfinite)
        .thenComparingLong(ChargeCollectorManager::getCollectorPositionKey);
    private final Map<BlockPos, ChargeCollectorBlockEntity> chargeCollectors = new HashMap<>();
    private final Map<BlockPos, InfiniteCollectorBlockEntity> infiniteCollectors = new HashMap<>();
    private final Map<Long, Set<ChargeCollectorBlockEntity>> chargeCollectorsByChunk = new HashMap<>();
    private final Map<Long, Set<InfiniteCollectorBlockEntity>> infiniteCollectorsByChunk = new HashMap<>();

    @Getter
    private final Level level;

    public ChargeCollectorManager(Level level) {
        this.level = level;
    }

    /**
     * 获取当前维度的ChargeCollectorManager
     */
    public static ChargeCollectorManager getInstance(Level level) {
        return INSTANCES.computeIfAbsent(level, ChargeCollectorManager::new);
    }

    public static void clear(Level level) {
        INSTANCES.remove(level);
    }

    public static void removeChargeCollector(Level level, ChargeCollectorBlockEntity blockEntity) {
        ChargeCollectorManager instance = INSTANCES.get(level);
        if (instance != null) instance.removeChargeCollector(blockEntity);
    }

    /**
     * 删除集电器
     */
    public void removeChargeCollector(ChargeCollectorBlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getBlockPos();
        if (chargeCollectors.get(blockPos) != blockEntity) return;
        chargeCollectors.remove(blockPos);
        removeChargeCollectorFromChunks(blockEntity);
    }

    public static void removeInfiniteCollector(Level level, InfiniteCollectorBlockEntity blockEntity) {
        ChargeCollectorManager instance = INSTANCES.get(level);
        if (instance != null) instance.removeInfiniteCollector(blockEntity);
    }

    /**
     * 删除无限收集器
     */
    public void removeInfiniteCollector(InfiniteCollectorBlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getBlockPos();
        if (infiniteCollectors.get(blockPos) != blockEntity) return;
        infiniteCollectors.remove(blockPos);
        removeInfiniteCollectorFromChunks(blockEntity);
    }

    /**
     * 充电
     *
     * @param chargeNum 充电量
     * @param level     维度
     * @param blockPos  充电的位置
     */
    public static void charge(double chargeNum, Level level, BlockPos blockPos) {
        if (!(chargeNum > 0) || level.isClientSide) return;
        ChargeCollectorManager instance = INSTANCES.get(level);
        if (instance == null) return;
        instance.charge(chargeNum, blockPos);
    }

    /**
     * 充电
     *
     * @param chargeNum 充电量
     * @param blockPos  充电的位置
     */
    public void charge(double chargeNum, BlockPos blockPos) {
        if (!(chargeNum > 0) || (chargeCollectors.isEmpty() && infiniteCollectors.isEmpty())) return;
        Collection<Entry> chargeCollectorCollection = this.getChargeCandidates(blockPos);
        if (chargeCollectorCollection.isEmpty()) return;
        double surplus = chargeNum;
        for (Entry entry : chargeCollectorCollection) {
            if (entry.isInfinite()) {
                InfiniteCollectorBlockEntity ic = entry.getInfiniteCollector();
                if (!this.canCollect(ic, blockPos)) continue;
                surplus = ic.incomingCharge(surplus, blockPos);
            } else {
                ChargeCollectorBlockEntity cc = entry.getChargeCollector();
                if (!this.canCollect(cc, blockPos)) continue;
                surplus = cc.incomingCharge(surplus, blockPos);
            }
            if (surplus == 0) return;
        }
    }

    /**
     * 添加新的集电器
     */
    public void addChargeCollector(ChargeCollectorBlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getBlockPos();
        ChargeCollectorBlockEntity previous = chargeCollectors.put(blockPos, blockEntity);
        if (previous != null && previous != blockEntity) {
            removeChargeCollectorFromChunks(previous);
        }
        addChargeCollectorToChunks(blockEntity);
    }

    /**
     * 添加新的无限收集器
     */
    public void addInfiniteCollector(InfiniteCollectorBlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getBlockPos();
        InfiniteCollectorBlockEntity previous = infiniteCollectors.put(blockPos, blockEntity);
        if (previous != null && previous != blockEntity) {
            removeInfiniteCollectorFromChunks(previous);
        }
        addInfiniteCollectorToChunks(blockEntity);
    }

    /**
     * 获取最近的收集器的List集合(以从近至远排序)
     */
    public List<Entry> getNearestChargeCollect(BlockPos blockPos) {
        List<Entry> distanceList = new ArrayList<>(chargeCollectors.size() + infiniteCollectors.size());
        for (Map.Entry<BlockPos, ChargeCollectorBlockEntity> entry : chargeCollectors.entrySet()) {
            distanceList.add(new Entry(distance(entry.getKey(), blockPos), entry.getValue(), null));
        }
        for (Map.Entry<BlockPos, InfiniteCollectorBlockEntity> entry : infiniteCollectors.entrySet()) {
            distanceList.add(new Entry(distance(entry.getKey(), blockPos), null, entry.getValue()));
        }
        if (distanceList.size() > 1) distanceList.sort(ENTRY_COMPARATOR);
        return distanceList;
    }

    private List<Entry> getChargeCandidates(BlockPos blockPos) {
        long chunkPos = ChunkPos.asLong(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        Set<ChargeCollectorBlockEntity> chargeCandidates = chargeCollectorsByChunk.get(chunkPos);
        Set<InfiniteCollectorBlockEntity> infiniteCandidates = infiniteCollectorsByChunk.get(chunkPos);
        if ((chargeCandidates == null || chargeCandidates.isEmpty())
            && (infiniteCandidates == null || infiniteCandidates.isEmpty())) {
            return List.of();
        }
        int candidateCount = (chargeCandidates == null ? 0 : chargeCandidates.size())
            + (infiniteCandidates == null ? 0 : infiniteCandidates.size());
        List<Entry> distanceList = new ArrayList<>(candidateCount);
        if (chargeCandidates != null) {
            for (ChargeCollectorBlockEntity collector : chargeCandidates) {
                if (collector.isRemoved() || !this.canCollect(collector, blockPos)) continue;
                distanceList.add(new Entry(distance(collector.getBlockPos(), blockPos), collector, null));
            }
        }
        if (infiniteCandidates != null) {
            for (InfiniteCollectorBlockEntity collector : infiniteCandidates) {
                if (collector.isRemoved() || !this.canCollect(collector, blockPos)) continue;
                distanceList.add(new Entry(distance(collector.getBlockPos(), blockPos), null, collector));
            }
        }
        if (distanceList.size() > 1) distanceList.sort(ENTRY_COMPARATOR);
        return distanceList;
    }

    private static long getCollectorPositionKey(Entry entry) {
        return entry.isInfinite()
            ? entry.getInfiniteCollector().getBlockPos().asLong()
            : entry.getChargeCollector().getBlockPos().asLong();
    }

    private static double distance(BlockPos first, BlockPos second) {
        double x = first.getX() - second.getX();
        double y = first.getY() - second.getY();
        double z = first.getZ() - second.getZ();
        return Math.sqrt(x * x + y * y + z * z);
    }

    private void addChargeCollectorToChunks(ChargeCollectorBlockEntity blockEntity) {
        addToChunks(
            this.chargeCollectorsByChunk,
            blockEntity,
            blockEntity.getRange()
        );
    }

    private void removeChargeCollectorFromChunks(ChargeCollectorBlockEntity blockEntity) {
        removeFromChunks(
            this.chargeCollectorsByChunk,
            blockEntity,
            blockEntity.getRange()
        );
    }

    private void addInfiniteCollectorToChunks(InfiniteCollectorBlockEntity blockEntity) {
        addToChunks(
            this.infiniteCollectorsByChunk,
            blockEntity,
            blockEntity.getRange()
        );
    }

    private void removeInfiniteCollectorFromChunks(InfiniteCollectorBlockEntity blockEntity) {
        removeFromChunks(
            this.infiniteCollectorsByChunk,
            blockEntity,
            blockEntity.getRange()
        );
    }

    private static <T extends BlockEntity> void addToChunks(
        Map<Long, Set<T>> collectorsByChunk,
        T blockEntity,
        int range
    ) {
        BlockPos blockPos = blockEntity.getBlockPos();
        int minChunkX = (blockPos.getX() - range) >> 4;
        int maxChunkX = (blockPos.getX() + range) >> 4;
        int minChunkZ = (blockPos.getZ() - range) >> 4;
        int maxChunkZ = (blockPos.getZ() + range) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                collectorsByChunk
                    .computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), ignored -> new HashSet<>())
                    .add(blockEntity);
            }
        }
    }

    private static <T extends BlockEntity> void removeFromChunks(
        Map<Long, Set<T>> collectorsByChunk,
        T blockEntity,
        int range
    ) {
        BlockPos blockPos = blockEntity.getBlockPos();
        int minChunkX = (blockPos.getX() - range) >> 4;
        int maxChunkX = (blockPos.getX() + range) >> 4;
        int minChunkZ = (blockPos.getZ() - range) >> 4;
        int maxChunkZ = (blockPos.getZ() + range) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long key = ChunkPos.asLong(chunkX, chunkZ);
                Set<T> collectors = collectorsByChunk.get(key);
                if (collectors == null) continue;
                collectors.remove(blockEntity);
                if (collectors.isEmpty()) collectorsByChunk.remove(key);
            }
        }
    }

    /**
     * 判断是否能被集电器收集
     *
     * @param blockEntity 集电器方块实体
     * @param blockPos    电荷的位置
     * @return 是否能被集点器收集
     */
    public boolean canCollect(ChargeCollectorBlockEntity blockEntity, BlockPos blockPos) {
        BlockPos collectorPos = blockEntity.getPos();
        int range = blockEntity.getRange();
        return collectorPos.getX() - range <= blockPos.getX()
            && collectorPos.getY() - range <= blockPos.getY()
            && collectorPos.getZ() - range <= blockPos.getZ()
            && collectorPos.getX() + range >= blockPos.getX()
            && collectorPos.getY() + range >= blockPos.getY()
            && collectorPos.getZ() + range >= blockPos.getZ();
    }

    /**
     * 判断是否能被无限收集器收集
     *
     * @param blockEntity 无限收集器方块实体
     * @param blockPos    电荷的位置
     * @return 是否能被收集
     */
    public boolean canCollect(InfiniteCollectorBlockEntity blockEntity, BlockPos blockPos) {
        int range = blockEntity.getRange();
        BlockPos collectorPos = blockEntity.getPos();
        return collectorPos.getX() - range <= blockPos.getX()
            && collectorPos.getY() - range <= blockPos.getY()
            && collectorPos.getZ() - range <= blockPos.getZ()
            && collectorPos.getX() + range >= blockPos.getX()
            && collectorPos.getY() + range >= blockPos.getY()
            && collectorPos.getZ() + range >= blockPos.getZ();
    }

    @Getter
    public static class Entry {
        public final double distance;
        public final @Nullable ChargeCollectorBlockEntity chargeCollector;
        public final @Nullable InfiniteCollectorBlockEntity infiniteCollector;

        public Entry(
            double distance,
            @Nullable ChargeCollectorBlockEntity chargeCollector,
            @Nullable InfiniteCollectorBlockEntity infiniteCollector
        ) {
            this.distance = distance;
            this.chargeCollector = chargeCollector;
            this.infiniteCollector = infiniteCollector;
        }

        public boolean isInfinite() {
            return this.infiniteCollector != null;
        }

    }
}
