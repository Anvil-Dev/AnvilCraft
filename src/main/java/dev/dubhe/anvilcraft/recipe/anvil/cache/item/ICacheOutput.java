package dev.dubhe.anvilcraft.recipe.anvil.cache.item;

import net.minecraft.world.item.ItemStack;

/**
 * 缓存输出接口
 */
public interface ICacheOutput {
    /**
     * 增加指定物品堆
     *
     * @param stack 物品堆
     * @param spawn 是否生成
     * @return 剩余的物品堆
     */
    ItemStack grow(ItemStack stack, boolean spawn);

    /**
     * 回滚增加操作
     *
     * @return 回滚的物品堆
     */
    ItemStack rollbackGrow();

    /**
     * 同步更改
     */
    void sync();
}
