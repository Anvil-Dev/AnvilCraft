package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

/**
 * Input wrapper for {@link PlanetResourceRecipe#matches}.
 * Carries the celestial body data and age anvil count for condition checking.
 */
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
