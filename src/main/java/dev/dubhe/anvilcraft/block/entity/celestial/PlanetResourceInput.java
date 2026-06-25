package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

/// {@link PlanetResourceRecipe#matches} 的输入包装器。
/// 携带天体数据和年龄砧子计数用于条件检查。
public record PlanetResourceInput(CelestialBodyData body, int ageAnvilCount) implements RecipeInput {

    @Override
    public @NotNull ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
