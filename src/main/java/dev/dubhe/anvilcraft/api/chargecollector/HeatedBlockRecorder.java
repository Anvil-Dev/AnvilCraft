package dev.dubhe.anvilcraft.api.chargecollector;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HeatedBlockRecorder {
    public static final Map<Block, HeatingInfo> TRANSFORMS = new HashMap<>();
    private static final Map<LevelAccessor, HeatedBlockRecorder> INSTANCES = new HashMap<>();

    static {
        TRANSFORMS.put(Blocks.NETHERITE_BLOCK, new HeatingInfo(ModBlocks.HEATED_NETHERITE.get(), 4, 0));
        TRANSFORMS.put(ModBlocks.HEATED_NETHERITE.get(), new HeatingInfo(ModBlocks.REDHOT_NETHERITE.get(), 12, 4));
        TRANSFORMS.put(ModBlocks.REDHOT_NETHERITE.get(), new HeatingInfo(ModBlocks.GLOWING_NETHERITE.get(), 32, 12));
        TRANSFORMS.put(ModBlocks.GLOWING_NETHERITE.get(), new HeatingInfo(ModBlocks.INCANDESCENT_NETHERITE.get(), 80, 32));
        TRANSFORMS.put(ModBlocks.INCANDESCENT_NETHERITE.get(), new HeatingInfo(null, Integer.MAX_VALUE, 80));

        TRANSFORMS.put(ModBlocks.TUNGSTEN_BLOCK.get(), new HeatingInfo(ModBlocks.HEATED_TUNGSTEN.get(), 4, 0));
        TRANSFORMS.put(ModBlocks.HEATED_TUNGSTEN.get(), new HeatingInfo(ModBlocks.REDHOT_TUNGSTEN.get(), 12, 4));
        TRANSFORMS.put(ModBlocks.REDHOT_TUNGSTEN.get(), new HeatingInfo(ModBlocks.GLOWING_TUNGSTEN.get(), 32, 12));
        TRANSFORMS.put(ModBlocks.GLOWING_TUNGSTEN.get(), new HeatingInfo(ModBlocks.INCANDESCENT_TUNGSTEN.get(), 80, 32));
        TRANSFORMS.put(ModBlocks.INCANDESCENT_TUNGSTEN.get(), new HeatingInfo(null, Integer.MAX_VALUE, 80));
    }

    private final LevelAccessor level;
    private final List<BlockEntity> irritateEntity = new ArrayList<>();
    private final Map<BlockPos, AtomicInteger> record = new HashMap<>();

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

    public static void clear() {
        INSTANCES.clear();
    }

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
        Block heatedBlock = this.level.getBlockState(pos).getBlock();
        while (true) {
            HeatingInfo info = TRANSFORMS.get(heatedBlock);
            if (info == null || info.nextTier == null || level < info.toNextTier) break;
            heatedBlock = info.nextTier;
        }
        this.level.setBlock(pos, heatedBlock.defaultBlockState(), 3);
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
            Block heatedBlock = newState.getBlock();
            while (true) {
                HeatingInfo info = TRANSFORMS.get(heatedBlock);
                if (info == null || info.nextTier == null || level < info.toNextTier) break;
                heatedBlock = info.nextTier;
            }
            this.level.setBlock(pos, heatedBlock.defaultBlockState(), 3);
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

    public record HeatingInfo(@Nullable Block nextTier, int toNextTier, int remainCurrentTier) {

    }
}
