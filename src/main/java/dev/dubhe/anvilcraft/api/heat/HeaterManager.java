package dev.dubhe.anvilcraft.api.heat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.util.Util;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static dev.dubhe.anvilcraft.api.power.PowerGrid.GRID_TICK;

public class HeaterManager {
    private static final Map<Level, HeaterManager> INSTANCES = new HashMap<>();

    private final Level level;
    private final Set<BlockPos> heatableBlocks = Collections.synchronizedSet(new HashSet<>());
    private final Multimap<HeaterInfo<?>, BlockPos> producers = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    public static HeaterManager getInstance(Level level) {
        if (level.isClientSide) return new HeaterManager(level);
        if (!INSTANCES.containsKey(level)) {
            INSTANCES.put(level, new HeaterManager(level));
        }
        return INSTANCES.get(level);
    }

    public HeaterManager(Level level) {
        this.level = level;
    }

    public static void addHeatableBlock(BlockPos pos, Level level) {
        HeaterManager.getInstance(level).heatableBlocks.add(pos);
    }

    public static void addProducer(BlockPos pos, Level level, HeaterInfo<?> info) {
        synchronized (HeaterManager.getInstance(level).producers) {
            HeaterManager.getInstance(level).producers.put(info, pos);
        }
    }

    public static void removeProducer(BlockPos pos, Level level, HeaterInfo<?> info) {
        synchronized (HeaterManager.getInstance(level).producers) {
            HeaterManager.getInstance(level).producers.remove(info, pos);
        }
    }

    public static void tickAll() {
        INSTANCES.forEach((level, manager) -> {
            if (level.getGameTime() % GRID_TICK != 0) return;
            if (level.tickRateManager().isFrozen() && !level.tickRateManager().isSteppingForward()) return;
            manager.tick();
        });
    }

    public void tick() {
        Multimap<HeaterInfo<?>, BlockPos> producers;
        synchronized (this.producers) {
            producers = MultimapBuilder.hashKeys().arrayListValues().build(this.producers);
        }
        Map<BlockPos, HeatTierLine.Point> heatableBlocks = new HashMap<>();
        for (HeaterInfo<?> info : producers.keySet()) {
            List<BlockPos> removals = new ArrayList<>();
            this.tickProducers(info, heatableBlocks, removals);
            for (BlockPos removal : removals) {
                this.producers.remove(info, removal);
            }
        }

        Set<BlockPos> poses = new HashSet<>();
        poses.addAll(this.heatableBlocks);
        poses.addAll(heatableBlocks.keySet());
        for (BlockPos pos : poses) {
            this.tickHeatableBlock(pos, heatableBlocks.get(pos));
        }
    }

    private <T> void tickProducers(
        HeaterInfo<T> info,
        Map<BlockPos, HeatTierLine.Point> heatableBlocks,
        List<BlockPos> removals
    ) {
        Collection<BlockPos> producerPoses = this.producers.get(info);
        Object2IntMap<BlockPos> heatablePosesAndProducerCount = new Object2IntArrayMap<>();
        for (BlockPos producerPos : producerPoses) {
            Optional<T> producerOp = info.getter().apply(this.level, producerPos);
            if (producerOp.isEmpty()) {
                removals.add(producerPos);
            }
            producerOp.map(producer -> new Pair<>(info.posesGetter().apply(producer), info.countGetter().applyAsInt(producer)))
                .ifPresent(
                    pair -> pair.getFirst()
                        .forEach(heatablePos -> heatablePosesAndProducerCount.mergeInt(heatablePos, pair.getSecond(), Integer::sum))
                );
        }
        for (BlockPos heatablePos : heatablePosesAndProducerCount.keySet()) {
            Optional<HeatTierLine.Point> pointOp = info.line().getPoint(heatablePosesAndProducerCount.getInt(heatablePos));
            if (pointOp.isEmpty()) continue;
            heatableBlocks.merge(heatablePos, pointOp.get(), HeatTierLine.Point::merge);
        }
    }

    private void tickHeatableBlock(BlockPos pos, @Nullable HeatTierLine.Point point) {
        this.heatableBlocks.remove(pos);
        BlockState heatableState = this.level.getBlockState(pos);
        if (!heatableState.is(ModBlockTags.HEATABLE_BLOCKS)) return;
        this.heatableBlocks.add(pos);

        HeatableBlockEntity heatable = Util.castSafely(this.level.getBlockEntity(pos), HeatableBlockEntity.class).orElse(null);
        Optional<ResourceLocation> idOp = HeatRecorder.getId(this.level, pos, heatableState);
        if (idOp.isEmpty()) return;
        HeatTier tier = HeatRecorder.getTier(this.level, pos, heatableState)
            .orElseThrow(() -> new IllegalStateException("Unexpected non tier heatable block!"));
        HeatTier tierDelta = Optional.ofNullable(point).map(HeatTierLine.Point::tier).orElse(tier);
        int durationDelta = Optional.ofNullable(point).map(HeatTierLine.Point::duration).orElse(0);
        if (tierDelta.compareTo(tier) > 0) {
            Optional<Block> deltaBlockOp = HeatRecorder.getHeatableBlock(idOp.get(), tierDelta);
            if (deltaBlockOp.isEmpty()) return;
            Block deltaBlock = deltaBlockOp.get();
            this.level.setBlock(pos, deltaBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (!(deltaBlock instanceof EntityBlock deltaEntityBlock)) return;
            BlockEntity deltaBlockEntity = deltaEntityBlock.newBlockEntity(pos, deltaBlock.defaultBlockState());
            if (!(deltaBlockEntity instanceof HeatableBlockEntity heatableEntity)) return;
            this.level.setBlockEntity(heatableEntity);
            this.level.updateNeighbourForOutputSignal(pos, deltaBlock);
            heatable = heatableEntity;
        } else if (tierDelta.compareTo(tier) < 0) {
            durationDelta = 0;
        }
        if (heatable == null) return;

        if (durationDelta == 0) {
            heatable.addDuration(-1);
        } else {
            heatable.addDurationInTick(durationDelta);
        }

        if (heatable.getDuration() <= 0) {
            Optional<BlockState> prevTierOp = heatable.getPrevTier(this.level, pos);
            if (prevTierOp.isEmpty()) return;
            BlockState prevState = prevTierOp.get();
            this.level.setBlock(pos, prevState, Block.UPDATE_CLIENTS);
            if (!(prevState.getBlock() instanceof EntityBlock)) return;
            BlockEntity tierEntity = this.level.getBlockEntity(pos);
            if (!(tierEntity instanceof HeatableBlockEntity heatableEntity)) return;
            heatableEntity.addDuration(10);
            this.level.setBlockEntity(heatableEntity);
            this.level.updateNeighbourForOutputSignal(pos, prevState.getBlock());
        }
    }

}
