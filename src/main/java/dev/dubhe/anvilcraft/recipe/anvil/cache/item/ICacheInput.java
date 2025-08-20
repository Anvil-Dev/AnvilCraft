package dev.dubhe.anvilcraft.recipe.anvil.cache.item;

/**
 * 缓存输入接口
 */
public interface ICacheInput {
    /**
     * 减少指定数量的物品
     *
     * @param count 数量
     * @return 剩余数量
     */
    int shrink(int count);

    /**
     * 回滚减少操作
     *
     * @return 回滚的数量
     */
    int rollbackShrink();

    /**
     * 同步更改
     */
    void sync();

    /**
     * 获取物品数量
     *
     * @return 物品数量
     */
    int getCount();
}
