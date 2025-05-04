package dev.dubhe.anvilcraft.api.item;

import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

/**
 * 多合一配方的结果
 */
public interface IMultipleToOneSmithingRecipeResult {
    /**
     * 定义多合一时对输入物品的操作逻辑
     *
     * @param id 配方对于该结果的id，在配方构建时设置
     * @param input 配方输入，包含模板、材料和其它输入
     * @return 结果
     */
    ItemStack assemble(int id, MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries);
}
