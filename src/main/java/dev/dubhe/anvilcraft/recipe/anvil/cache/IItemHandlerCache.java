package dev.dubhe.anvilcraft.recipe.anvil.cache;

import net.neoforged.neoforge.items.IItemHandler;

/**
 * 物品处理器缓存接口，用于定义输入和输出物品处理器的访问方法
 * 实现该接口的类可以提供对输入和输出物品处理器的访问
 */
public interface IItemHandlerCache {
    /**
     * 获取输入物品处理器
     *
     * @return 输入物品处理器
     */
    IItemHandler getInput();

    /**
     * 获取输出物品处理器
     *
     * @return 输出物品处理器
     */
    IItemHandler getOutput();
}