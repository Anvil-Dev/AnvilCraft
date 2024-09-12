package dev.dubhe.anvilcraft.api.chargecollector;

import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HeatedBlockRecorder {
    private static final Map<LevelAccessor, HeatedBlockRecorder> INSTANCES = new HashMap<>();
    private static final Map<Pair<Block, Integer>, BlockFamily> TRANSFORMS = new HashMap<>();

    static {
        BlockFamily tungstenFamily = new BlockFamily(List.of(
                ModBlocks.TUNGSTEN_BLOCK.get(),
                ModBlocks.HEATED_TUNGSTEN.get(),
                ModBlocks.REDHOT_TUNGSTEN.get(),
                ModBlocks.GLOWING_TUNGSTEN.get(),
                ModBlocks.INCANDESCENT_TUNGSTEN.get()));
        BlockFamily netheriteFamily = new BlockFamily(List.of(
                Blocks.NETHERITE_BLOCK,
                ModBlocks.HEATED_NETHERITE.get(),
                ModBlocks.REDHOT_NETHERITE.get(),
                ModBlocks.GLOWING_NETHERITE.get(),
                ModBlocks.INCANDESCENT_NETHERITE.get()));

        BlockFamily emberMetalFamily = new BlockFamily(
                List.of(ModBlocks.EMBER_METAL_BLOCK.get(), ModBlocks.CUT_EMBER_METAL_BLOCK.get()));

        TRANSFORMS.put(Pair.of(ModBlocks.HEATED_TUNGSTEN.get(), 2), tungstenFamily);
        TRANSFORMS.put(Pair.of(ModBlocks.REDHOT_TUNGSTEN.get(), 8), tungstenFamily);
        TRANSFORMS.put(Pair.of(ModBlocks.GLOWING_TUNGSTEN.get(), 32), tungstenFamily);
        TRANSFORMS.put(Pair.of(ModBlocks.INCANDESCENT_TUNGSTEN.get(), 128), tungstenFamily);

        TRANSFORMS.put(Pair.of(ModBlocks.HEATED_NETHERITE.get(), 2), netheriteFamily);
        TRANSFORMS.put(Pair.of(ModBlocks.REDHOT_NETHERITE.get(), 8), netheriteFamily);
        TRANSFORMS.put(Pair.of(ModBlocks.GLOWING_NETHERITE.get(), 32), netheriteFamily);
        TRANSFORMS.put(Pair.of(ModBlocks.INCANDESCENT_NETHERITE.get(), 128), netheriteFamily);

        TRANSFORMS.put(Pair.of(ModBlocks.EMBER_METAL_BLOCK.get(), 8), emberMetalFamily);
        TRANSFORMS.put(Pair.of(ModBlocks.CUT_EMBER_METAL_BLOCK.get(), 2), emberMetalFamily);
    }

    private final LevelAccessor level;
    private final List<BlockEntity> irritateEntity = new ArrayList<>();

    private HeatedBlockRecorder(LevelAccessor level) {
        this.level = level;
    }

    /**
     * 获取当前level的HeatedBlockRecorder实例
     */
    public static HeatedBlockRecorder getInstance(LevelAccessor level) {
        if (!INSTANCES.containsKey(level)) {
            INSTANCES.put(level, new HeatedBlockRecorder(level));
        }
        return INSTANCES.get(level);
    }

    private final Map<BlockPos, AtomicInteger> record = new HashMap<>();

    /**
     * 记录方块照射
     */
    public void addOrIncrease(BlockPos pos, BlockEntity entity) {
        if (irritateEntity.contains(entity)) return;
        if (!record.containsKey(pos)) {
            record.put(pos, new AtomicInteger(0));
        }
        int level = record.get(pos).addAndGet(1);
        irritateEntity.add(entity);
        List<Block> blocks = TRANSFORMS.entrySet().stream()
                .filter(it -> it.getValue().anyMatch(this.level.getBlockState(pos).getBlock()))
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingInt(Pair::right))
                .filter(it -> it.right() <= level)
                .map(Pair::left)
                .collect(Collectors.toCollection(ArrayList::new));
        if (blocks.isEmpty()) return;
        Block block = blocks.get(blocks.size() - 1);
        this.level.setBlock(pos, block.defaultBlockState(), 3);
    }

    /**
     * 移除方块照射
     */
    public void remove(BlockPos pos, BlockEntity entity) {
        if (!irritateEntity.contains(entity)) return;
        if (!record.containsKey(pos)) {
            record.put(pos, new AtomicInteger(0));
            return;
        }
        AtomicInteger integer = record.get(pos);
        integer.set(Mth.clamp(integer.intValue() - 1, 0, 0x7fffffff));
        if (integer.intValue() <= 0) {
            record.remove(pos);
        }
        irritateEntity.remove(entity);
    }

    /**
     * 方塊改變
     */
    @SuppressWarnings("unused")
    public void onBlockStateChange(BlockPos pos, BlockState blockState, BlockState newState) {
        if (record.containsKey(pos)) {
            int level = record.get(pos).get();
            List<Block> blocks = TRANSFORMS.entrySet().stream()
                    .filter(it -> it.getValue().anyMatch(newState.getBlock()))
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.comparingInt(Pair::right))
                    .filter(it -> it.right() <= level)
                    .map(Pair::left)
                    .collect(Collectors.toCollection(ArrayList::new));
            if (blocks.isEmpty()) return;
            Block block = blocks.get(blocks.size() - 1);
            this.level.setBlock(pos, block.defaultBlockState(), 3);
        }
    }

    /**
     * 检查光照强度
     */
    public boolean requireLightLevel(BlockPos pos, int level) {
        if (!record.containsKey(pos)) {
            return false;
        }
        return record.get(pos).get() >= level;
    }

    public static void clear() {
        INSTANCES.clear();
    }

    public record BlockFamily(List<Block> blocks) {
        public boolean anyMatch(Block block) {
            return blocks.stream().anyMatch(it -> it == block);
        }
    }
}
