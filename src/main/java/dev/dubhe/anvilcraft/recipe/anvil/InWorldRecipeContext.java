package dev.dubhe.anvilcraft.recipe.anvil;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 世界内配方上下文类，用于存储和管理世界内配方执行过程中的数据和状态
 * 该类实现了 RecipeInput 接口，提供了配方执行所需的各种信息和方法
 */
public class InWorldRecipeContext implements RecipeInput {
    /**
     * 服务器世界实例
     */
    @Getter
    private final ServerLevel level;

    /**
     * 配方执行位置
     */
    @Getter
    private final Vec3 pos;

    /**
     * 相关实体
     */
    @Getter
    private final Entity entity;

    /**
     * 存储配方数据的映射表
     */
    private final Map<ResourceLocation, Object> data = new ConcurrentHashMap<>();

    /**
     * 存储接受者的映射表
     */
    private final Map<ResourceLocation, Consumer<InWorldRecipeContext>> acceptors = new ConcurrentHashMap<>();

    /**
     * 配方谓词堆栈
     */
    @Getter
    private final List<IRecipePredicate<?>> stack = Collections.synchronizedList(new LinkedList<>());

    /**
     * 构造一个新的世界内配方上下文
     *
     * @param level  服务器世界实例
     * @param pos    配方执行位置
     * @param entity 相关实体
     */
    public InWorldRecipeContext(ServerLevel level, Vec3 pos, Entity entity) {
        this.level = level;
        this.pos = pos;
        this.entity = entity;
    }

    /**
     * 将配方谓词推入堆栈
     *
     * @param predicate 要推入的配方谓词
     */
    public void push(@NotNull IRecipePredicate<?> predicate) {
        this.stack.add(predicate);
        predicate.snapshot(this);
    }

    /**
     * 从堆栈中弹出配方谓词
     *
     * @param predicate 要弹出的配方谓词
     */
    public void pop(@NotNull IRecipePredicate<?> predicate) {
        predicate.rollback(this);
        this.stack.removeLast();
    }

    /**
     * 将数据存储到上下文中
     *
     * @param key   数据键
     * @param value 数据值
     * @param <T>   数据类型
     */
    public <T> void put(@NotNull InWorldRecipeData<T> key, T value) {
        this.data.put(key.location(), value);
    }

    /**
     * 从上下文中获取数据
     *
     * @param key 数据键
     * @param <T> 数据类型
     * @return 数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull InWorldRecipeData<T> key) {
        T value = (T) this.data.get(key.location());
        return value != null ? value : key.supplier().apply(this, key);
    }

    /**
     * 如果指定键不存在，则计算并存储新值
     *
     * @param key 数据键
     * @param <T> 数据类型
     * @return 数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(@NotNull InWorldRecipeData<T> key) {
        return (T) this.data.computeIfAbsent(key.location(), k -> key.supplier().apply(this, key));
    }

    /**
     * 获取指定索引的物品堆，默认返回空物品堆
     *
     * @param i 索引
     * @return 物品堆
     */
    @Override
    public @NotNull ItemStack getItem(int i) {
        return ItemStack.EMPTY;
    }

    /**
     * 获取物品堆列表的大小，默认为0
     *
     * @return 大小
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * 判断物品堆列表是否为空，默认为true
     *
     * @return 是否为空
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * 将接受者存储到上下文中
     *
     * @param key      接受者键
     * @param acceptor 接受者
     */
    public void putAcceptor(ResourceLocation key, @NotNull Consumer<InWorldRecipeContext> acceptor) {
        this.acceptors.put(key, acceptor);
    }

    /**
     * 执行所有接受者
     */
    public void accept() {
        this.acceptors.values().forEach(acceptor -> acceptor.accept(this));
    }

    /**
     * 创建一个空的战利品上下文
     *
     * @return 战利品上下文
     */
    public @NotNull LootContext emptyLootContext() {
        return new LootContext.Builder(new LootParams(this.level, Map.of(), Map.of(), 0)).create(Optional.empty());
    }

    /**
     * 获取数字提供器的浮点数值，并限制在指定范围内
     *
     * @param provider 数字提供器
     * @param min 最小值
     * @param max 最大值
     * @return 浮点数值
     */
    public float getFloat(@NotNull NumberProvider provider, float min, float max) {
        return Math.clamp(provider.getFloat(this.emptyLootContext()), min, max);
    }

    /**
     * 获取数字提供器的浮点数值，并限制最大值
     *
     * @param provider 数字提供器
     * @param max 最大值
     * @return 浮点数值
     */
    public float getFloat(@NotNull NumberProvider provider, float max) {
        return Math.min(provider.getFloat(this.emptyLootContext()), max);
    }

    /**
     * 获取数字提供器的浮点数值
     *
     * @param provider 数字提供器
     * @return 浮点数值
     */
    public float getFloat(@NotNull NumberProvider provider) {
        return provider.getFloat(this.emptyLootContext());
    }

    /**
     * 获取数字提供器的整数数值，并限制在指定范围内
     *
     * @param provider 数字提供器
     * @param min 最小值
     * @param max 最大值
     * @return 整数数值
     */
    public int getInt(@NotNull NumberProvider provider, int min, int max) {
        return Math.clamp(provider.getInt(this.emptyLootContext()), min, max);
    }

    /**
     * 获取数字提供器的整数数值，并限制最大值
     *
     * @param provider 数字提供器
     * @param max 最大值
     * @return 整数数值
     */
    public int getInt(@NotNull NumberProvider provider, int max) {
        return Math.min(provider.getInt(this.emptyLootContext()), max);
    }

    /**
     * 获取数字提供器的整数数值
     *
     * @param provider 数字提供器
     * @return 整数数值
     */
    public int getInt(@NotNull NumberProvider provider) {
        return provider.getInt(this.emptyLootContext());
    }
}