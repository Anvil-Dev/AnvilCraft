package dev.dubhe.anvilcraft.recipe.anvil.cache.item;


import dev.dubhe.anvilcraft.recipe.anvil.cache.ItemCache;
import dev.dubhe.anvilcraft.recipe.anvil.cache.item.operation.CacheOperation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

/**
 * 抽象缓存元素类，实现了缓存元素接口
 */
public abstract class AbstractCacheElement implements ICacheElement {
    /**
     * 物品缓存
     */
    protected final ItemCache cache;

    /**
     * 物品类型
     */
    protected final ItemStack type;

    /**
     * 模拟的物品堆
     */
    protected ItemStack simulate;

    /**
     * 增加模拟栈
     */
    protected final Deque<CacheOperation> growSimulateStack = new ArrayDeque<>();

    /**
     * 减少模拟栈
     */
    protected final Deque<CacheOperation> shrinkSimulateStack = new ArrayDeque<>();

    /**
     * 构造一个新的抽象缓存元素
     *
     * @param cache    物品缓存
     * @param simulate 模拟的物品堆
     */
    protected AbstractCacheElement(ItemCache cache, ItemStack simulate) {
        this.cache = cache;
        this.simulate = simulate;
        this.type = simulate.copyWithCount(1);
    }

    /**
     * 减少指定数量的物品
     *
     * @param count 数量
     * @return 剩余数量
     */
    @Override
    public int shrink(int count) {
        int shrink = Math.min(this.simulate.getCount(), count);
        this.simulate.shrink(shrink);
        this.shrinkSimulateStack.add(new CacheOperation(shrink));
        return count - shrink;
    }

    /**
     * 增加指定物品堆
     *
     * @param stack 物品堆
     * @param spawn 是否生成
     * @return 剩余的物品堆
     */
    @Override
    public ItemStack grow(@NotNull ItemStack stack, boolean spawn) {
        ItemStack copy = stack.copy();
        if (!this.is(stack)) return copy;
        int growCount = copy.getCount();
        int simulateCount = this.simulate.getCount();
        int grownSimulateCount = Math.min(this.getCapacity(stack), simulateCount + growCount);
        int grownCount = grownSimulateCount - simulateCount;
        int remainingCount = growCount - grownCount;
        if (remainingCount > 0) copy.setCount(remainingCount);
        else copy = ItemStack.EMPTY;
        if (!this.simulate.isEmpty()) {
            this.simulate.grow(grownCount);
        } else {
            this.simulate = this.type.copy();
            this.simulate.setCount(grownCount);
        }
        this.growSimulateStack.push(new CacheOperation(grownCount));
        return copy;
    }

    /**
     * 回滚增加操作
     *
     * @return 回滚的物品堆
     */
    @Override
    public ItemStack rollbackGrow() {
        CacheOperation operation = this.growSimulateStack.pop();
        ItemStack copy = this.simulate.copy();
        this.simulate.shrink(operation.amount());
        copy.setCount(operation.amount());
        return copy;
    }

    /**
     * 回滚减少操作
     *
     * @return 回滚的数量
     */
    @Override
    public int rollbackShrink() {
        CacheOperation operation = this.shrinkSimulateStack.pop();
        this.simulate.grow(operation.amount());
        return operation.amount();
    }

    /**
     * 判断是否满足指定谓词条件
     *
     * @param stack 物品谓词
     * @return 是否满足条件
     */
    public boolean is(Predicate<ItemStack> stack) {
        return stack.test(this.type);
    }

    /**
     * 判断是否为指定物品堆
     *
     * @param stack 物品堆
     * @return 是否为指定物品堆
     */
    @Override
    public boolean is(@Nullable ItemStack stack) {
        if (stack == null) return false;
        return ItemStack.isSameItemSameComponents(stack, this.type);
    }

    /**
     * 获取物品数量
     *
     * @return 物品数量
     */
    @Override
    public int getCount() {
        return this.simulate.isEmpty() ? 0 : this.simulate.getCount();
    }
}
