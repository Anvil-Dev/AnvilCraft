package dev.dubhe.anvilcraft.api.heat.collector;

import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.InfiniteCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import it.unimi.dsi.fastutil.doubles.Double2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static dev.dubhe.anvilcraft.api.power.PowerGrid.GRID_TICK;

public class HeatCollectorManager {
    private static final Map<Level, HeatCollectorManager> INSTANCES = new HashMap<>();
    private static final List<HeatSourceEntry> SOURCE_ENTRIES = new ArrayList<>();

    static {
        registerEntry(HeatSourceEntry.predicateAlways(4, state -> state.is(ModBlockTags.HEATED_BLOCKS)));
        registerEntry(HeatSourceEntry.predicateAlways(16, state -> state.is(ModBlockTags.REDHOT_BLOCKS)));
        registerEntry(HeatSourceEntry.predicateAlways(64, state -> state.is(ModBlockTags.GLOWING_BLOCKS)));
        registerEntry(HeatSourceEntry.predicateAlways(256, state -> state.is(ModBlockTags.INCANDESCENT_BLOCKS)));
        registerEntry(HeatSourceEntry.predicateAlways(1024, state -> state.is(ModBlockTags.OVERHEATED_BLOCKS)));

        registerEntry(HeatSourceEntry.simple(2, Blocks.MAGMA_BLOCK, Blocks.NETHERRACK));
        registerEntry(HeatSourceEntry.predicate(
            4,
            CampfireBlock::isLitCampfire,
            it -> it.setValue(CampfireBlock.LIT, false)
        ));
        registerEntry(HeatSourceEntry.predicate(
            4,
            state -> state.getFluidState().isSourceOfType(Fluids.LAVA),
            it -> Blocks.OBSIDIAN.defaultBlockState()
        ));
        registerEntry(HeatSourceEntry.simple(4, Blocks.LAVA_CAULDRON, ModBlocks.OBSIDIAN_CAULDRON.get()));

        registerEntry(HeatSourceEntry.predicateAlways(2, state -> state.is(ModBlockTags.STORAGE_BLOCKS_URANIUM)));
        registerEntry(HeatSourceEntry.forever(4, ModBlocks.EMBER_METAL_BLOCK.get()));
        registerEntry(HeatSourceEntry.predicateAlways(8, state -> state.is(ModBlockTags.STORAGE_BLOCKS_PLUTONIUM)));
    }

    private final Level level;
    private final Set<BlockPos> heatCollectors = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> infiniteCollectors = Collections.synchronizedSet(new HashSet<>());

    public static void clear() {
        INSTANCES.clear();
    }

    /**
     * 获取当前维度的HeatCollectorManager
     */
    public static HeatCollectorManager getInstance(Level level) {
        synchronized (INSTANCES) {
            if (INSTANCES.get(level) == null) {
                HeatCollectorManager.INSTANCES.put(level, new HeatCollectorManager(level));
            }
            return HeatCollectorManager.INSTANCES.get(level);
        }
    }

    public static void registerEntry(HeatSourceEntry entry) {
        SOURCE_ENTRIES.add(entry);
    }

    public static Optional<HeatSourceEntry> getEntry(BlockState state) {
        for (HeatSourceEntry entry : HeatCollectorManager.SOURCE_ENTRIES) {
            if (entry.accepts(state) > 0) return Optional.of(entry);
        }
        return Optional.empty();
    }

    public static void addHeatCollector(BlockPos pos, Level level) {
        getInstance(level).heatCollectors.add(pos);
    }

    public static void removeHeatCollector(BlockPos pos, Level level) {
        getInstance(level).heatCollectors.remove(pos);
    }

    public static void addInfiniteCollector(BlockPos pos, Level level) {
        getInstance(level).infiniteCollectors.add(pos);
    }

    public static void removeInfiniteCollector(BlockPos pos, Level level) {
        getInstance(level).infiniteCollectors.remove(pos);
    }

    HeatCollectorManager(Level level) {
        this.level = level;
    }

    public static void tickAll() {
        INSTANCES.values().forEach(HeatCollectorManager::tick);
    }

    private void tick() {
        if (level.isClientSide) {
            return;
        }
        if (this.level.getGameTime() % GRID_TICK != 0) return;
        List<IHeatCollector> collectors = this.getCollectorsFromNWToSE();
        Map<Entry, Double2ObjectMap<IHeatCollector>> heatSources = new HashMap<>();
        for (IHeatCollector collector : collectors) {
            this.collectSources(collector, heatSources);
        }
        for (Entry entry : heatSources.keySet()) {
            int heat = entry.accepts();
            for (IHeatCollector entity : heatSources.get(entry).values()) {
                heat = entity.inputHeat(heat);
                if (heat == 0) break;
            }
            if (this.level.getGameTime() % entry.entry().timeToTransform() == 0) {
                this.level.setBlockAndUpdate(entry.pos(), entry.transform());
            }
        }
    }

    private void collectSources(IHeatCollector collector, Map<Entry, Double2ObjectMap<IHeatCollector>> heatSources) {
        BlockPos collectorPos = collector.getCollectorPos();
        int collectorRange = collector.getCollectorRange();
        int overlapRange = collectorRange + 1;
        Map<Entry, Double2ObjectMap<IHeatCollector>> heatSourcesCache = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(
            collectorPos.above(overlapRange).east(overlapRange).south(overlapRange),
            collectorPos.below(overlapRange).west(overlapRange).north(overlapRange)
        )) {
            pos = pos.immutable();
            BlockState state = this.level.getBlockState(pos);
            // Check for other collectors (both heat and infinite) in range
            if (!pos.equals(collectorPos)) {
                boolean isNearHeatCollector = state.is(ModBlocks.HEAT_COLLECTOR);
                boolean isNearInfiniteCollector = state.is(ModBlocks.INFINITE_COLLECTOR);
                if (isNearHeatCollector || isNearInfiniteCollector) {
                    collector.setCollectorWorking(false);
                    return;
                }
            }
            if (Math.abs(pos.getX() - collectorPos.getX()) > collectorRange
                || Math.abs(pos.getY() - collectorPos.getY()) > collectorRange
                || Math.abs(pos.getZ() - collectorPos.getZ()) > collectorRange
            ) {
                continue;
            }
            BlockPos finalPos = pos;
            getEntry(state)
                .ifPresent(entry -> {
                    heatSourcesCache.computeIfAbsent(new Entry(finalPos, state, entry), it -> new Double2ObjectAVLTreeMap<>())
                        .put(
                            Vector3i.distance(
                                finalPos.getX(), finalPos.getY(), finalPos.getZ(),
                                collector.getCollectorPos().getX(), collector.getCollectorPos().getY(), collector.getCollectorPos().getZ()
                            ), collector
                    );
                    TriggerUtil.heatCollectOn(this.level, finalPos, state, this.level.getBlockEntity(finalPos));
                });
        }
        collector.setCollectorWorking(true);
        for (var entry : heatSourcesCache.entrySet()) {
            heatSources
                .computeIfAbsent(entry.getKey(), it -> new Double2ObjectAVLTreeMap<>())
                .putAll(entry.getValue());
        }
    }

    /**
     * 获取收集器的List集合(以从西北到东南排序)
     */
    private List<IHeatCollector> getCollectorsFromNWToSE() {
        List<IHeatCollector> collectors = new ArrayList<>();
        for (Iterator<BlockPos> iterator = this.heatCollectors.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            BlockEntity be = this.level.getBlockEntity(pos);
            if (be instanceof HeatCollectorBlockEntity hc) {
                collectors.add(hc);
            } else {
                iterator.remove();
            }
        }
        for (Iterator<BlockPos> iterator = this.infiniteCollectors.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            BlockEntity be = this.level.getBlockEntity(pos);
            if (be instanceof InfiniteCollectorBlockEntity ic) {
                collectors.add(ic);
            } else {
                iterator.remove();
            }
        }
        collectors.sort(Comparator.comparing(IHeatCollector::getCollectorPos));
        return collectors;
    }

    private record Entry(BlockPos pos, BlockState state, HeatSourceEntry entry) {
        public int accepts() {
            return this.entry.accepts(this.state);
        }

        public BlockState transform() {
            return this.entry.transform(this.state);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof Entry entry1 && this.pos.equals(entry1.pos));
        }
    }
}
