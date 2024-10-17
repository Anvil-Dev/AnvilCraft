package dev.dubhe.anvilcraft.inventory.container;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import lombok.Getter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class JewelSourceContainer extends SimpleContainer {
    @Getter
    private RecipeHolder<JewelCraftingRecipe> recipe;
    private final JewelCraftingMenu menu;

    public JewelSourceContainer(JewelCraftingMenu menu) {
        super(1);
        this.menu = menu;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        recipe = RecipeCaches.getJewelRecipeByResult(stack);
        this.menu.slotsChanged(this);
    }
}
