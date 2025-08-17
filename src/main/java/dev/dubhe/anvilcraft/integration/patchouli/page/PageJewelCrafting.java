package dev.dubhe.anvilcraft.integration.patchouli.page;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliRenderHelper;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

import java.util.List;

public class PageJewelCrafting extends PageDoubleRecipeRegistry<JewelCraftingRecipe> {
    public PageJewelCrafting() {
        super(ModRecipeTypes.JEWEL_CRAFTING_TYPE.get());
    }

    @Override
    protected void drawRecipe(
        GuiGraphics graphics, JewelCraftingRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second
    ) {
        RenderHelper.renderBlock(
            graphics,
            ModBlocks.JEWEL_CRAFTING_TABLE.getDefaultState(),
            recipeX + 50, recipeY + 15, 0,
            12,
            RenderHelper.SINGLE_BLOCK
        );

        PatchouliRenderHelper.render1x1(graphics, recipeX - 4, recipeY + 7);
        PatchouliRenderHelper.renderItemStack(parent, graphics, recipe.result, recipeX, recipeY + 11, mouseX, mouseY);
        PatchouliRenderHelper.render1x4(graphics, recipeX - 4, recipeY + 33);
        List<Object2IntMap.Entry<Ingredient>> ingredients = recipe.getMergedIngredients();
        for (int i = 0; i < Math.min(ingredients.size(), 4); i++) {
            PatchouliRenderHelper.renderIngredientWithCount(
                parent, graphics, ingredients.get(i), recipeX + i * 19, recipeY + 37, mouseX, mouseY
            );
        }

        PatchouliRenderHelper.renderArray(graphics, recipeX + 25, recipeY + 15);
        PatchouliRenderHelper.renderArray(graphics, recipeX + 66, recipeY + 15);

        parent.drawCenteredStringNoShadow(
            graphics, getTitle(second).getVisualOrderText(),
            GuiBook.PAGE_WIDTH / 2, recipeY - 5,
            book.headerColor
        );

        PatchouliRenderHelper.render1x1(graphics, recipeX + 80, recipeY + 7);
        PatchouliRenderHelper.renderItemStack(parent, graphics, recipe.result, recipeX + 84, recipeY + 11, mouseX, mouseY);
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, JewelCraftingRecipe recipe) {
        return recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected int getRecipeHeight() {
        return 78;
    }
}
