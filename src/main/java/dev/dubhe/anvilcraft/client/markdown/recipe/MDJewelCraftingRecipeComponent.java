package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.resources.Identifier;

import java.util.List;

public class MDJewelCraftingRecipeComponent extends MDRecipeComponent {
    public static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(AnvilCraft.MOD_ID, "textures/gui/ageratum/jewelcrafting_table.png");

    private final ItemIngredientPredicate result;
    private final List<ItemIngredientPredicate> ingredients;

    public MDJewelCraftingRecipeComponent(JewelCraftingRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 142, 62, enableAlignCenter);
        this.result = recipe.source();
        this.ingredients = recipe.ingredients();
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        AgeratumUtil.renderItemWithoutSlot(context, this.result, mouseX, mouseY, 65, 9);
        AgeratumUtil.renderItemWithoutSlot(context, this.result, mouseX, mouseY, 117, 37);

        for (int i = 0; i < Math.min(this.ingredients.size(), 4); i++) {
            AgeratumUtil.renderItemWithoutSlot(context, this.ingredients.get(i), mouseX, mouseY, 8 + i * AgeratumUtil.SLOT_SIZE, 37);
        }
    }

}
