package dev.dubhe.anvilcraft.integration.patchouli.page;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

import java.util.List;

public class PageItemInject extends PageDoubleRecipeRegistry<ItemInjectRecipe> {
    public PageItemInject() {
        super(Util.cast(ModRecipeTypes.ITEM_INJECT_TYPE.get()));
    }

    @Override
    protected void drawRecipe(
        GuiGraphics graphics, ItemInjectRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second
    ) {
        PatchouliRenderHelper.render1x1(graphics, recipeX - 4, recipeY + 16);

        List<ItemIngredientPredicate> ingredients = recipe.getInputItems();
        PatchouliRenderHelper.renderIngredient(parent, graphics, ingredients.getFirst(), recipeX, recipeY + 20, mouseX, mouseY);

        PatchouliRenderHelper.renderArray(graphics, recipeX + 25, recipeY + 20);

        PatchouliRenderHelper.renderAnvilWithAnimation(parent, graphics, recipeX + 50, recipeY + 15);

        List<BlockState> states = recipe.getBlockIngredient().constructStatesForRender();
        if (!states.isEmpty()) {
            RenderHelper.renderBlock(
                graphics, states.get((parent.ticksInBook / 20) % states.size()),
                recipeX + 50, recipeY + 31, 0,
                12,
                RenderHelper.SINGLE_BLOCK);
        }

        PatchouliRenderHelper.renderArray(graphics, recipeX + 66, recipeY + 20);

        RenderHelper.renderBlock(
            graphics, recipe.getBlockResult().getState(),
            recipeX + 90, recipeY + 31, 0,
            12,
            RenderHelper.SINGLE_BLOCK);

        parent.drawCenteredStringNoShadow(
            graphics, getTitle(second).getVisualOrderText(),
            GuiBook.PAGE_WIDTH / 2, recipeY - 5,
            book.headerColor);
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, ItemInjectRecipe recipe) {
        return recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected int getRecipeHeight() {
        return 78;
    }
}
