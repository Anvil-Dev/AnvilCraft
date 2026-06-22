package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

/**
 * Input wrapper for {@link SpecialCelestialBodyRecipe#matches}.
 * Carries no actual items — matching is done externally via anvil counts and seed item.
 */
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
