package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * {@link SpecialCelestialBodyRecipe#matches} 的空输入包装；
 * 实际匹配由外部使用砧子数量和种子物品完成。
 */
public record SpecialCelestialBodyInput() implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
