package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MDMeshRecipeComponent extends MDRecipeComponent {
    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/ageratum/backgrounds.png");

    @Getter
    @Nullable
    private final List<ItemIngredientPredicate> mergedIngredients;
    @Getter
    @Nullable
    private final List<ChanceItemStack> resultItems;

    public MDMeshRecipeComponent(MeshRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 256, 128, enableAlignCenter);  // 纹理尺寸（原始像素）
        mergedIngredients = recipe.getInputItems();
        resultItems = recipe.getResultItems();
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphics g = context.graphics();
        if (mergedIngredients == null || resultItems == null) return;

        RENDER_INGREDIENT: {
            if (mergedIngredients.isEmpty()) break RENDER_INGREDIENT;
            ItemIngredientPredicate ingredient = mergedIngredients.getFirst();
            ItemStack displaying = ingredient.getItems()[0];
            if (displaying.isEmpty()) break RENDER_INGREDIENT;
            AgeratumUtil.renderItem(context, g, displaying, mouseX, mouseY, 6, 18);
        }
        AgeratumUtil.renderArrow(g, 18, 8);
        AgeratumUtil.renderBlock(g, Blocks.ANVIL.defaultBlockState(), 56, 10);
        AgeratumUtil.renderBlock(g, Blocks.SCAFFOLDING.defaultBlockState(), 56, 34);
        AgeratumUtil.renderArrow(g, 40, 40, 90);
        RENDER_RESULT: {
            if (resultItems.isEmpty()) break RENDER_RESULT;
            int size = resultItems.size();
            for (int i = 0; i < size; i++) {
                ChanceItemStack chanceItemStack = resultItems.get(i);
                ItemStack stack = chanceItemStack.stack().copy();
                double count = AgeratumUtil.getMax(chanceItemStack.count());
                stack.setCount((int) count);
                int x = 6 + i * AgeratumUtil.SLOT_SIZE;
                AgeratumUtil.renderItem(context, g, stack, mouseX, mouseY, x, 76);
            }
        }
    }
}
