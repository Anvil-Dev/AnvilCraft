package dev.dubhe.anvilcraft.recipe.anvil.cache.item;


import dev.dubhe.anvilcraft.recipe.anvil.cache.ItemCache;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.operation.InputOutputOperation;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.operation.SpawnOperation;
import dev.dubhe.anvilcraft.recipe.anvil.util.Range;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 缓存输入输出实现类，实现了缓存输入和输出接口
 */
public class ICacheInputOutputImpl implements ICacheInput, ICacheOutput {
    /**
     * 物品缓存
     */
    private final ItemCache cache;

    /**
     * 元素集合
     */
    private final Set<ICacheElement> elements = new HashSet<>();

    /**
     * 增加模拟栈
     */
    private final Deque<InputOutputOperation> growSimulateStack = new ArrayDeque<>();

    /**
     * 减少模拟栈
     */
    private final Deque<InputOutputOperation> shrinkSimulateStack = new ArrayDeque<>();

    /**
     * 生成模拟栈
     */
    private final Deque<SpawnOperation> spawnSimulateStack = new ArrayDeque<>();

    /**
     * 位置
     */
    private final Vec3 pos;

    /**
     * 键
     */
    private final Object key;

    /**
     * 范围
     */
    private final Range range;

    /**
     * 构造一个新的缓存输入输出实现
     *
     * @param key      键
     * @param cache    物品缓存
     * @param pos      位置
     * @param range    范围
     * @param elements 元素集合
     */
    public ICacheInputOutputImpl(Object key, ItemCache cache, Vec3 pos, Range range, Collection<ICacheElement> elements) {
        this.key = key;
        this.cache = cache;
        this.pos = pos;
        this.range = range;
        this.elements.addAll(elements);
    }

    /**
     * 减少指定数量的物品
     *
     * @param count 数量
     * @return 剩余数量
     */
    @Override
    public int shrink(int count) {
        Set<ICacheElement> elements = new HashSet<>();
        for (ICacheElement element : this.elements) {
            count = element.shrink(count);
            elements.add(element);
            if (count <= 0) break;
        }
        this.shrinkSimulateStack.push(new InputOutputOperation(elements));
        return count;
    }

    /**
     * 回滚减少操作
     *
     * @return 回滚的数量
     */
    @Override
    public int rollbackShrink() {
        InputOutputOperation pop = this.shrinkSimulateStack.pop();
        int count = 0;
        for (ICacheElement element : pop.elements()) {
            count += element.rollbackShrink();
        }
        return count;
    }

    /**
     * 增加指定物品堆
     *
     * @param stack 物品堆
     * @param spawn 是否生成
     * @return 剩余的物品堆
     */
    @Override
    public ItemStack grow(ItemStack stack, boolean spawn) {
        Set<ICacheElement> elements = new HashSet<>();
        for (ICacheElement element : this.elements) {
            stack = element.grow(stack, false);
            elements.add(element);
            if (stack.isEmpty()) break;
        }
        this.growSimulateStack.push(new InputOutputOperation(elements));
        if (spawn) {
            this.spawnSimulateStack.push(new SpawnOperation(stack.copy(), stack.getCount(), this.pos));
            return ItemStack.EMPTY;
        } else {
            this.spawnSimulateStack.push(new SpawnOperation(ItemStack.EMPTY, 0, this.pos));
            return stack;
        }
    }

    /**
     * 回滚增加操作
     *
     * @return 回滚的物品堆
     */
    @Override
    public ItemStack rollbackGrow() {
        InputOutputOperation operation = this.growSimulateStack.pop();
        ItemStack stack = ItemStack.EMPTY;
        for (ICacheElement element : operation.elements()) {
            ItemStack stack1 = element.rollbackGrow();
            if (stack.isEmpty()) stack = stack1;
            else stack.grow(stack1.getCount());
        }
        SpawnOperation spawnOperation = this.spawnSimulateStack.pop();
        stack.grow(spawnOperation.count());
        return stack;
    }

    /**
     * 同步更改
     */
    @Override
    public void sync() {
        this.growSimulateStack.clear();
        this.shrinkSimulateStack.clear();
        this.cache.pushSpawnList(this.spawnSimulateStack);
        this.spawnSimulateStack.clear();
        this.elements.forEach(ICacheElement::sync);
    }

    /**
     * 获取物品数量
     *
     * @return 物品数量
     */
    @Override
    public int getCount() {
        return this.elements.stream().mapToInt(ICacheElement::getCount).sum();
    }

    /**
     * 判断是否等于指定键和范围
     *
     * @param key   键
     * @param range 范围
     * @return 是否相等
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean equals(@Nullable Object key, Range range) {
        if (key == null) return false;
        if (key.getClass() != this.key.getClass()) return false;
        if (!this.range.equals(range)) return false;
        if (key instanceof ItemStack stack && this.key instanceof ItemStack stack1) {
            return ItemStack.isSameItemSameComponents(stack, stack1);
        }
        return this.key.equals(key);
    }
}
