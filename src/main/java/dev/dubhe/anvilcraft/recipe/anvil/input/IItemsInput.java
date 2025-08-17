package dev.dubhe.anvilcraft.recipe.anvil.input;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 物品输入接口，用于定义配方的物品输入
 * 实现该接口的类可以提供物品堆列表作为配方的输入
 */
public interface IItemsInput {
    /**
     * 获取物品堆列表
     *
     * @return 物品堆列表
     */
    List<ItemStack> items();
}