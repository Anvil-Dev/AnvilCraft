package dev.dubhe.anvilcraft.api.heat.collector;

import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.Util;
import it.unimi.dsi.fastutil.doubles.Double2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
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
            t -> t.setValue(CampfireBlock.LIT, false)
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

    public static void checkWhenPlaceCollector(BlockPlaceContext ctx, BlockPos pos, Level level) {
        HeatCollectorManager manager = getInstance(level);
        AABB validRange = AABB.ofSize(pos.getCenter(), 9, 9, 9);
        for (BlockPos checkedPos : manager.heatCollectors) {
            if (validRange.contains(checkedPos.getCenter())) {
                Optional.ofNullable(ctx.getPlayer()).ifPresent(player -> player.displayClientMessage(
                    Component.translatable("block.anvilcraft.heat_collector.placement_too_close_to_another")
                        .withStyle(ChatFormatting.RED), true));
                manager.heatCollectors.add(pos);
                return;
            }
        }
        manager.heatCollectors.add(pos);
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
        List<HeatCollectorBlockEntity> collectors = this.getCollectorsFromNWToSE();
        Map<Entry, Double2ObjectMap<HeatCollectorBlockEntity>> heatSources = new HashMap<>();
        for (HeatCollectorBlockEntity collector : collectors) {
            this.collectSources(collector, heatSources);
        }
        for (Entry entry : heatSources.keySet()) {
            int heat = entry.accepts();
            for (HeatCollectorBlockEntity entity : heatSources.get(entry).values()) {
                heat = entity.inputtingHeat(heat);
                if (heat == 0) break;
            }
            if (this.level.getGameTime() % entry.entry().timeToTransform() == 0) {
                this.level.setBlockAndUpdate(entry.pos(), entry.transform());
            }
        }
    }

    private void collectSources(HeatCollectorBlockEntity collector, Map<Entry, Double2ObjectMap<HeatCollectorBlockEntity>> heatSources) {
        BlockPos collectorPos = collector.getPos();
        Map<Entry, Double2ObjectMap<HeatCollectorBlockEntity>> heatSourcesCache = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(
            collectorPos.above(4).east(4).south(4),
            collectorPos.below(4).west(4).north(4))
        ) {
            pos = pos.immutable();
            BlockState state = this.level.getBlockState(pos);
            if (state.is(ModBlocks.HEAT_COLLECTOR) && !pos.equals(collectorPos)) {
                collector.setResult(HeatCollectorBlockEntity.WorkResult.TOO_CLOSE);
                //heatSources.values().removeIf(map -> map.values().removeIf(entity -> entity.equals(collector)));
                return;
            }
            if (Math.abs(pos.getX() - collectorPos.getX()) > 2
                || Math.abs(pos.getY() - collectorPos.getY()) > 2
                || Math.abs(pos.getZ() - collectorPos.getZ()) > 2
            ) continue;
            BlockPos finalPos = pos;
            getEntry(state)
                .ifPresent(entry -> heatSourcesCache
                    .computeIfAbsent(
                        new Entry(finalPos, state, entry), it -> new Double2ObjectAVLTreeMap<>())
                    .put(
                        Vector3i.distance(
                            finalPos.getX(), finalPos.getY(), finalPos.getZ(),
                            collector.getPos().getX(), collector.getPos().getY(), collector.getPos().getZ()
                        ), collector
                    )
                );
        }
        collector.setResult(HeatCollectorBlockEntity.WorkResult.SUCCESS);
        for (var entry : heatSourcesCache.entrySet()) {
            heatSources
                .computeIfAbsent(entry.getKey(), it -> new Double2ObjectAVLTreeMap<>())
                .putAll(entry.getValue());
        }
    }

    /**
     * 获取集热器的List集合(以从西北到东南排序)
     */
    private List<HeatCollectorBlockEntity> getCollectorsFromNWToSE() {
        List<HeatCollectorBlockEntity> collectors = new ArrayList<>();
        for (Iterator<BlockPos> iterator = this.heatCollectors.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            Util.castSafely(this.level.getBlockEntity(pos), HeatCollectorBlockEntity.class)
                .ifPresentOrElse(collectors::add, iterator::remove);
        }
        collectors.sort(Comparator.comparing(BlockEntity::getBlockPos));
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
