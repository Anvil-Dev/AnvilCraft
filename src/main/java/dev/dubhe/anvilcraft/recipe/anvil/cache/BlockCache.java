package dev.dubhe.anvilcraft.recipe.anvil.cache;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * 方块缓存类，用于在配方执行过程中缓存和模拟方块状态变化
 * 该类提供了对方块状态的读取、修改和提交功能，确保配方执行过程中的数据一致性
 */
public class BlockCache {
    /**
     * 方块缓存的数据键
     */
    public static final InWorldRecipeData<BlockCache> BLOCK_CACHE = InWorldRecipeData.of(AnvilCraft.of("block_cache"), BlockCache::of);

    /**
     * 默认接受者
     */
    public static final Consumer<InWorldRecipeContext> DEFAULT_ACCEPTOR = (ctx) -> ctx.get(BlockCache.BLOCK_CACHE).accept();

    /**
     * 模拟的方块状态映射表
     */
    private final HashMap<BlockPos, BlockState> simulated = new HashMap<>();

    /**
     * 模拟的方块实体映射表
     */
    private final HashMap<BlockPos, BlockEntity> simulatedEntity = new HashMap<>();

    /**
     * 缓存的方块状态映射表
     */
    private final HashMap<BlockPos, BlockState> cache = new HashMap<>();

    /**
     * 缓存的方块实体映射表
     */
    private final HashMap<BlockPos, BlockEntity> cacheEntity = new HashMap<>();

    /**
     * 世界访问器
     */
    private final LevelAccessor level;

    /**
     * 构造一个新的方块缓存
     *
     * @param level 世界访问器
     */
    public BlockCache(LevelAccessor level) {
        this.level = level;
    }

    /**
     * 创建一个新的方块缓存实例
     *
     * @param level 配方上下文
     * @param key   方块缓存数据键
     * @return 方块缓存实例
     */
    private static @NotNull BlockCache of(@NotNull InWorldRecipeContext level, InWorldRecipeData<BlockCache> key) {
        return new BlockCache(level.getLevel());
    }

    /**
     * 获取指定位置的方块状态
     *
     * @param pos 方块位置
     * @return 方块状态
     */
    public BlockState getBlockState(@NotNull BlockPos pos) {
        cache.computeIfAbsent(pos, level::getBlockState);
        cacheEntity.computeIfAbsent(pos, level::getBlockEntity);
        simulatedEntity.computeIfAbsent(pos, level::getBlockEntity);
        return simulated.computeIfAbsent(pos, level::getBlockState);
    }

    /**
     * 获取指定位置的方块实体
     *
     * @param pos 方块位置
     * @return 方块实体
     */
    public BlockEntity getBlockEntity(BlockPos pos) {
        cache.computeIfAbsent(pos, level::getBlockState);
        cacheEntity.computeIfAbsent(pos, level::getBlockEntity);
        simulated.computeIfAbsent(pos, level::getBlockState);
        return simulatedEntity.computeIfAbsent(pos, level::getBlockEntity);
    }

    /**
     * 设置指定位置的方块状态
     *
     * @param pos   方块位置
     * @param state 方块状态
     */
    public void setBlock(@NotNull BlockPos pos, BlockState state) {
        if (state == null) state = Blocks.AIR.defaultBlockState();
        this.getBlockState(pos);
        simulated.put(pos, state);
        simulatedEntity.put(pos, null);
    }

    /**
     * 设置指定位置的方块
     *
     * @param pos   方块位置
     * @param block 方块
     */
    public void setBlock(@NotNull BlockPos pos, Block block) {
        if (block == null) block = Blocks.AIR;
        this.setBlock(pos, block.defaultBlockState());
    }

    /**
     * 移除指定位置的方块
     *
     * @param pos 方块位置
     */
    public void removeBlock(@NotNull BlockPos pos) {
        this.setBlock(pos, Blocks.AIR);
    }

    /**
     * 提交所有模拟的方块更改到实际世界中
     */
    public void accept() {
        simulated.forEach((pos, state) -> {
            BlockState old = cache.get(pos);
            if (old == null) throw new IllegalStateException("Block at " + pos + " was not found in the cache!");
            if (old.equals(state)) return;
            level.setBlock(pos, state, 3);
            cache.put(pos, state);
        });
    }
}