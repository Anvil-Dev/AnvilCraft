package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe.MDCraftingTableRecipeComponent;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class MDVanillaCraftingComponent extends MDRecipeComponent {
    private final List<Optional<Ingredient>> ingredients;
    private final ItemStack resultItem;

    public MDVanillaCraftingComponent(CraftingRecipe recipe, boolean center) {
        super(MDCraftingTableRecipeComponent.CRAFTING_TABLE_COMPONENT_TEXTURE, 128, 72, center);
        var slots = new ArrayList<Optional<Ingredient>>(Collections.nCopies(9, Optional.empty()));
        if (recipe instanceof ShapedRecipe shaped) {
            for (int y = 0; y < Math.min(3, shaped.getHeight()); y++) {
                for (int x = 0; x < Math.min(3, shaped.getWidth()); x++) {
                    slots.set(x + y * 3, shaped.getIngredients().get(x + y * shaped.getWidth()));
                }
            }
            this.resultItem = shaped.result.create();
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            var inputs = shapeless.placementInfo().ingredients();
            for (int i = 0; i < Math.min(9, inputs.size()); i++) slots.set(i, Optional.of(inputs.get(i)));
            var result = shapeless.result();
            this.resultItem = result == null ? ItemStack.EMPTY : result.create();
        } else {
            this.resultItem = ItemStack.EMPTY;
        }
        this.ingredients = List.copyOf(slots);
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        for (int i = 0; i < this.ingredients.size(); i++) {
            var ingredient = this.ingredients.get(i);
            if (ingredient.isEmpty()) continue;
            this.draw(context, RecipeUtil.getDisplayItem(ingredient.get()), 9 + i % 3 * 19, 9 + i / 3 * 19, mouseX, mouseY);
        }
        this.draw(context, this.resultItem, 102, 28, mouseX, mouseY);
    }

    private void draw(MDRenderContext context, ItemStack stack, int x, int y, float mouseX, float mouseY) {
        if (stack.isEmpty()) return;
        context.graphics().item(stack, x, y);
        context.graphics().itemDecorations(Minecraft.getInstance().font, stack, x, y);
        this.extractTooltipRenderState(context, stack, x, y, mouseX, mouseY);
    }

    public List<Optional<Ingredient>> getIngredients() {
        return this.ingredients;
    }

    public ItemStack getResultItem() {
        return this.resultItem;
    }
}
