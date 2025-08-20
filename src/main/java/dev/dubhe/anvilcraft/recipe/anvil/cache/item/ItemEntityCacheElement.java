package dev.dubhe.anvilcraft.recipe.anvil.cache.item;

import dev.dubhe.anvilcraft.recipe.anvil.cache.ItemCache;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 物品实体缓存元素类，继承自抽象缓存元素类
 */
@EqualsAndHashCode(callSuper = false)
public class ItemEntityCacheElement extends AbstractCacheElement implements ICacheElement {
    /**
     * 物品实体
     */
    private final ItemEntity entity;

    /**
     * 是否在世界中
     */
    private boolean isInLevel;

    /**
     * 位置
     */
    @Getter
    private final Vec3 pos;

    /**
     * 构造一个新的物品实体缓存元素
     *
     * @param cache  物品缓存
     * @param entity 物品实体
     */
    public ItemEntityCacheElement(ItemCache cache, ItemEntity entity) {
        super(cache, entity.getItem().copy());
        this.pos = entity.position().add(0.0, 0.125, 0.0);
        this.entity = entity;
        this.isInLevel = true;
    }

    /**
     * 创建一个新的物品实体缓存元素
     *
     * @param cache 物品缓存
     * @param stack 物品堆
     * @param pos   位置
     * @return 物品实体缓存元素
     */
    public static @NotNull ItemEntityCacheElement create(@NotNull ItemCache cache, ItemStack stack, @NotNull Vec3 pos) {
        ItemEntity itemEntity = new ItemEntity(cache.getLevel(), pos.x, pos.y, pos.z, stack, 0, 0, 0);
        ItemEntityCacheElement element = new ItemEntityCacheElement(cache, itemEntity);
        element.isInLevel = false;
        element.simulate.setCount(0);
        return element;
    }

    /**
     * 同步更改
     */
    @Override
    public void sync() {
        this.growSimulateStack.clear();
        this.shrinkSimulateStack.clear();
        this.entity.setItem(this.simulate);
        if (this.isInLevel) return;
        this.entity.anvilcraft$setIsAdsorbable(false);
        this.cache.getLevel().addFreshEntity(this.entity);
    }

    /**
     * 获取指定物品堆的容量
     *
     * @param stack 物品堆
     * @return 容量
     */
    @Override
    public int getCapacity(ItemStack stack) {
        return this.simulate.isEmpty() ? stack.getMaxStackSize() : this.simulate.getMaxStackSize();
    }
}

