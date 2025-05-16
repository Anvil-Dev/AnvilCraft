package dev.dubhe.anvilcraft.integration.patchouli.page;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliRenderHelper;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.FourToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.TwoToOneSmithingRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

import java.util.List;

public class PageMultipleToOneSmithing extends PageDoubleRecipeRegistry<BaseMultipleToOneSmithingRecipe<?>> {
    public PageMultipleToOneSmithing() {
        super(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING_TYPE.get());
    }

    @Override
    protected void drawRecipe(GuiGraphics graphics, BaseMultipleToOneSmithingRecipe<?> recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second) {
        int vOffset = 186;
        if (recipe instanceof TwoToOneSmithingRecipe<?>) vOffset = 58;
        if (recipe instanceof FourToOneSmithingRecipe<?>) vOffset = 122;
        PatchouliRenderHelper.renderCraftingCustomUV(
            graphics,
            recipeX - 1, recipeY - 1,
            8, vOffset,
            100, 62
        );

        parent.drawCenteredStringNoShadow(
            graphics, getTitle(second).getVisualOrderText(),
            GuiBook.PAGE_WIDTH / 2, recipeY - 10,
            book.headerColor
        );

        parent.renderIngredient(graphics, recipeX + 22, recipeY + 22, mouseX, mouseY, recipe.getMaterial());

        parent.renderItemStack(graphics, recipeX + 79, recipeY + 22, mouseX, mouseY, recipe.getResultItem(RegistryAccess.EMPTY));
        parent.renderItemStack(graphics, recipeX + 79, recipeY + 41, mouseX, mouseY, recipe.getToastSymbol());

        List<Ingredient> ingredients = recipe.getIngredients();
        parent.renderIngredient(graphics, recipeX + 22, recipeY + 3, mouseX, mouseY, ingredients.getFirst());
        parent.renderIngredient(graphics, recipeX + 22, recipeY + 41, mouseX, mouseY, ingredients.get(1));
        if (recipe instanceof TwoToOneSmithingRecipe<?>) return;
        parent.renderIngredient(graphics, recipeX + 3, recipeY + 22, mouseX, mouseY, ingredients.get(2));
        parent.renderIngredient(graphics, recipeX + 41, recipeY + 22, mouseX, mouseY, ingredients.get(3));
        if (recipe instanceof FourToOneSmithingRecipe<?>) return;
        parent.renderIngredient(graphics, recipeX + 3, recipeY + 3, mouseX, mouseY, ingredients.get(4));
        parent.renderIngredient(graphics, recipeX + 41, recipeY + 3, mouseX, mouseY, ingredients.get(5));
        parent.renderIngredient(graphics, recipeX + 3, recipeY + 41, mouseX, mouseY, ingredients.get(6));
        parent.renderIngredient(graphics, recipeX + 41, recipeY + 41, mouseX, mouseY, ingredients.get(7));
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, BaseMultipleToOneSmithingRecipe<?> recipe) {
        return recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected int getRecipeHeight() {
        return 78;
    }
}
