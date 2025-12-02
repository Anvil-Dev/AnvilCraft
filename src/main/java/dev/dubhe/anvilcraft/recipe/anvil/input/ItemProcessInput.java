package dev.dubhe.anvilcraft.recipe.anvil.input;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * 物品处理输入记录类，用于封装配方处理过程中的物品输入
 * 该类实现了 RecipeInput 和 IItemsInput 接口，提供了对物品堆列表的访问
 */
public record ItemProcessInput(
    List<ItemStack> items // 物品堆列表
) implements RecipeInput, IItemsInput {
    /**
     * 获取指定索引的物品堆
     *
     * @param index 索引
     * @return 物品堆
     */
    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    /**
     * 获取物品堆列表的大小
     *
     * @return 大小
     */
    @Override
    public int size() {
        return items.size();
    }
}