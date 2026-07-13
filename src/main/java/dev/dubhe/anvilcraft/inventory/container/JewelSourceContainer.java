package dev.dubhe.anvilcraft.inventory.container;

import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import lombok.Getter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

public class JewelSourceContainer extends SimpleContainer {
    @Getter
    private @Nullable RecipeHolder<JewelCraftingRecipe> recipe;
    private final JewelCraftingMenu menu;

    public JewelSourceContainer(JewelCraftingMenu menu) {
        super(1);
        this.menu = menu;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        this.recipe = this.menu.findRecipeBySource(stack);
        this.menu.slotsChanged(this);
    }
}
