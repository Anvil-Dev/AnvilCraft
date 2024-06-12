package dev.dubhe.anvilcraft.api.chargecollector;

import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static dev.dubhe.anvilcraft.api.power.PowerGrid.GRID_TICK;

public class ThermoManager {
    private static final Map<Level, ThermoManager> instances = new HashMap<>();
    private final Set<ThermoBlock> thermoBlocks = new CopyOnWriteArraySet<>();
    private final List<ThermoEntry> thermoEntries = new ArrayList<>();
    private final Level level;

    public static void clear() {
        instances.clear();
    }

    /**
     * 获取当前维度的ThermoManager
     */
    public static ThermoManager getInstance(Level level) {
        synchronized (instances) {
            if (instances.get(level) == null) {
                ThermoManager.instances.put(level, new ThermoManager(level));
            }
            return ThermoManager.instances.get(level);
        }
    }

    private void register(ThermoEntry entry) {
        thermoEntries.add(entry);
    }

    /**
     * 添加新的热方块
     */
    public void addThermoBlock(BlockPos blockPos, BlockState state) {
        Optional<ThermoEntry> op =
                thermoEntries.stream().filter(it -> it.accepts(state) > 0).findFirst();
        if (op.isPresent()) {
            thermoBlocks.removeIf(it -> state.is(it.block) && blockPos.equals(it.pos));
            thermoBlocks.add(new ThermoBlock(blockPos, state.getBlock(), op.get().ttl()));
        }
    }

    ThermoManager(Level level) {
        this.level = level;
        register(ThermoEntry.simple(
                256, ModBlocks.INCANDESCENT_NETHERITE.get(), ModBlocks.GLOWING_NETHERITE.get()));
        register(ThermoEntry.simple(
                64, ModBlocks.GLOWING_NETHERITE.get(), ModBlocks.REDHOT_NETHERITE.get()));
        register(
                ThermoEntry.simple(16, ModBlocks.REDHOT_NETHERITE.get(), ModBlocks.HEATED_NETHERITE.get()));
        register(ThermoEntry.simple(4, ModBlocks.HEATED_NETHERITE.get(), Blocks.NETHERITE_BLOCK));

        register(ThermoEntry.simple(
                256, ModBlocks.INCANDESCENT_TUNGSTEN.get(), ModBlocks.GLOWING_TUNGSTEN.get()));
        register(
                ThermoEntry.simple(64, ModBlocks.GLOWING_TUNGSTEN.get(), ModBlocks.REDHOT_TUNGSTEN.get()));
        register(
                ThermoEntry.simple(16, ModBlocks.REDHOT_TUNGSTEN.get(), ModBlocks.HEATED_TUNGSTEN.get()));
        register(
                ThermoEntry.simple(4, ModBlocks.HEATED_TUNGSTEN.get(), ModBlocks.TUNGSTEN_BLOCK.get()));

        register(ThermoEntry.always(2, ModBlocks.URANIUM_BLOCK.get()));

        register(ThermoEntry.simple(4, Blocks.LAVA, Blocks.OBSIDIAN));
        register(ThermoEntry.simple(4, Blocks.MAGMA_BLOCK, Blocks.NETHERRACK));
        register(ThermoEntry.simple(4, Blocks.LAVA_CAULDRON, ModBlocks.OBSIDIDAN_CAULDRON.get()));

        register(ThermoEntry.predicate(
                4, CampfireBlock::isLitCampfire, t -> t.setValue(CampfireBlock.LIT, false)));
    }

    public static void tick() {
        instances.values().forEach(ThermoManager::tickThis);
    }

    private void tickThis() {
        if (this.level.getGameTime() % GRID_TICK != 0) return;
        List<ThermoBlock> removal = new ArrayList<>();
        for (ThermoBlock block : thermoBlocks) {
            BlockPos blockPos = block.pos;
            BlockState state = this.level.getBlockState(blockPos);
            Optional<ThermoEntry> optional =
                    thermoEntries.stream().filter(it -> it.accepts(state) > 0).findFirst();
            if (optional.isPresent()) {
                ThermoEntry entry = optional.get();
                if (block.ttl % 2 == 0) {
                    charge(entry.accepts(state), blockPos);
                }
                if (block.ttl > 0) {
                    block.decrease();
                } else {
                    level.setBlockAndUpdate(blockPos, entry.transform(state));
                    removal.add(block);
                }
            } else {
                removal.add(block);
            }
        }
        removal.forEach(thermoBlocks::remove);
    }

    private void charge(int chargeNum, BlockPos blockPos) {
        Collection<ChargeCollectorManager.Entry> chargeCollectorCollection =
                ChargeCollectorManager.getNearestChargeCollect(blockPos);
        double surplus = chargeNum;
        for (ChargeCollectorManager.Entry entry : chargeCollectorCollection) {
            ChargeCollectorBlockEntity chargeCollectorBlockEntity = entry.getBlockEntity();
            if (!ChargeCollectorManager.canCollect(chargeCollectorBlockEntity, blockPos)) return;
            surplus = chargeCollectorBlockEntity.incomingCharge(surplus);
            if (surplus == 0) return;
        }
    }

    private static class ThermoBlock {
        private final BlockPos pos;
        private final Block block;
        private int ttl;

        public ThermoBlock(BlockPos pos, Block block, int ttl) {
            this.pos = pos;
            this.block = block;
            this.ttl = ttl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ThermoBlock block1)) return false;
            return Objects.equals(pos, block1.pos) && Objects.equals(block, block1.block);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, block);
        }

        public void decrease() {
            ttl--;
        }
    }
}
