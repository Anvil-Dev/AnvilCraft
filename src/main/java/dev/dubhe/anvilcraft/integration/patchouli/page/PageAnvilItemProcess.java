package dev.dubhe.anvilcraft.integration.patchouli.page;

import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.util.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

import java.util.List;
import java.util.function.Function;

public class PageAnvilItemProcess<T extends Recipe<?>> extends PageDoubleRecipeRegistry<T> {
    private final Function<T, List<ItemIngredientPredicate>> ingredients;
    private final Function<T, List<ChanceItemStack>> results;
    private final Function<T, BlockState> state1;
    private final @Nullable Function<T, BlockState> state2;

    public PageAnvilItemProcess(
        RecipeType<T> recipeType,
        Function<T, List<ItemIngredientPredicate>> ingredients, Function<T, List<ChanceItemStack>> results,
        Function<T, BlockState> state1, @Nullable Function<T, BlockState> state2
    ) {
        super(recipeType);
        this.ingredients = ingredients;
        this.results = results;
        this.state1 = state1;
        this.state2 = state2;
    }

    @Override
    protected void drawRecipe(GuiGraphics graphics, T recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second) {
        boolean has2States = this.state2 != null;
        PatchouliRenderHelper.renderAnvilWithAnimation(parent, graphics, recipeX + 58, recipeY + 13 + (has2States ? 0 : 6));
        RenderHelper.renderBlock(
            graphics,
            this.state1.apply(recipe),
            recipeX + 58,
            recipeY + 29 + (has2States ? 0 : 6),
            10,
            12,
            RenderHelper.SINGLE_BLOCK
        );
        if (has2States) {
            RenderHelper.renderBlock(
                graphics,
                this.state2.apply(recipe),
                recipeX + 58,
                recipeY + 39,
                0,
                12,
                RenderHelper.SINGLE_BLOCK
            );
        }

        parent.drawCenteredStringNoShadow(
            graphics, getTitle(second).getVisualOrderText(),
            GuiBook.PAGE_WIDTH / 2, recipeY - 10,
            book.headerColor
        );

        PatchouliRenderHelper.renderArray(graphics, recipeX + 37, recipeY + 25);
        PatchouliRenderHelper.renderArray(graphics, recipeX + 70, recipeY + 25);

        List<ItemIngredientPredicate> inputs = this.ingredients.apply(recipe);
        if (inputs.size() <= 4) {
            PatchouliRenderHelper.render2x2(graphics, recipeX - 8, recipeY + 8);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2 && i * 2 + j < inputs.size(); j++) {
                    PatchouliRenderHelper.renderIngredient(
                        parent, graphics, inputs.get(i * 2 + j), recipeX - 4 + j * 19, recipeY + 12 + i * 19, mouseX, mouseY
                    );
                }
            }
        } else if (inputs.size() <= 6) {
            PatchouliRenderHelper.render3x2(graphics, recipeX - 6, recipeY);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3 && i * 2 + j < inputs.size(); j++) {
                    PatchouliRenderHelper.renderIngredient(
                        parent, graphics, inputs.get(i * 2 + j), recipeX - 4 + j * 19, recipeY + 4 + i * 19, mouseX, mouseY
                    );
                }
            }
        }

        List<ChanceItemStack> results = this.results.apply(recipe);
        if (results.size() == 1) {
            PatchouliRenderHelper.render1x1(graphics, recipeX + 81, recipeY + 18);
            parent.renderItemStack(graphics, recipeX + 85, recipeY + 22, mouseX, mouseY, results.getFirst().getStack());
        } else if (results.size() > 1) {
            PatchouliRenderHelper.render2x1(graphics, recipeX + 81, recipeY + 8);
            parent.renderItemStack(graphics, recipeX + 85, recipeY + 12, mouseX, mouseY, results.getFirst().getStack());
            parent.renderItemStack(graphics, recipeX + 85, recipeY + 31, mouseX, mouseY, results.get(1).getStack());
        }

        this.drawExtra(graphics, recipe, recipeX, recipeY, mouseX, mouseY, second);
    }

    @SuppressWarnings("unused")
    protected void drawExtra(GuiGraphics graphics, T recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second) {
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, T recipe) {
        return recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected int getRecipeHeight() {
        return 78;
    }
}
