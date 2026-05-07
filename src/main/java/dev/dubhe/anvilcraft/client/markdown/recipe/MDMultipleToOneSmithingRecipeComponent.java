package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.resources.ResourceLocation;

public class MDMultipleToOneSmithingRecipeComponent extends MDRecipeComponent {
    public static final int TEMPLATE_X = 8;
    public static final int Y = 28;
    public static final int CENTER_INPUT_X = 71;
    public static final int OUTPUT_X = 146;
    public static final int[] INPUT_X = {0, 0, -1, 1, -1, 1, -1, 1};
    public static final int[] INPUT_Y = {-1, 1, 0, 0, -1, -1, 1, 1};
    public static final int INPUT_SIZE = 19;
    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(AnvilCraft.MOD_ID, "textures/gui/ageratum/ember_smithing_table.png");
    private final BaseMultipleToOneSmithingRecipe recipe;

    public MDMultipleToOneSmithingRecipeComponent(BaseMultipleToOneSmithingRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 170, 72, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        AgeratumUtil.renderItemWithoutSlot(context, recipe.getTemplate(), mouseX, mouseY, TEMPLATE_X, Y);
        AgeratumUtil.renderItemWithoutSlot(context, recipe.getMaterial(), mouseX, mouseY, CENTER_INPUT_X, Y);

        for (int i = 0; i < Math.min(8, recipe.getInputs().size()); i++) {
            ItemIngredientPredicate ingredient = recipe.getInputs().get(i);
            int x = CENTER_INPUT_X + INPUT_X[i] * INPUT_SIZE;
            int y = Y + INPUT_Y[i] * INPUT_SIZE;
            AgeratumUtil.renderItemWithoutSlot(context, ingredient, mouseX, mouseY, x, y);
        }
        AgeratumUtil.renderItemWithoutSlot(context, recipe.getResult().result().getDefaultInstance(), mouseX, mouseY, OUTPUT_X, Y);
    }
}
