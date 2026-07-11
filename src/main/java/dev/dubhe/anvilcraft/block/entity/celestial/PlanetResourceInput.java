package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * {@link PlanetResourceRecipe#matches} 的输入，携带天体数据和时间砧子数量。
 */
public record PlanetResourceInput(CelestialBodyData body, int ageAnvilCount) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
