package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

/// {@link SpecialCelestialBodyRecipe#matches} 的输入包装器。
/// 不携带实际物品 —— 匹配在外部通过砧子计数和种子物品完成。
public record SpecialCelestialBodyInput() implements RecipeInput {

    @Override
    public @NotNull ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
