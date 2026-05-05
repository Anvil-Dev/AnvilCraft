package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MDMeshRecipeComponent extends MDRecipeComponent {
    private static final int SLOT_SIZE = 18;
    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/ageratum/backgrounds.png");
    public static final ResourceLocation ARROW =
        Ageratum.location("textures/gui/component/arrow.png");

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
            g.renderItem(displaying, 0, 10);
            g.renderItemDecorations(Minecraft.getInstance().font, displaying, 0, 10);
            this.renderTooltip(context, displaying, 0, 10, mouseX, mouseY);
        }
        g.blit(ARROW, 12, 1, 0, 0, 32, 32, 32, 32);
        RENDER_RESULT: {
            if (resultItems.isEmpty()) break RENDER_RESULT;
            int size = resultItems.size();
            for (int i = 0; i < size; i++) {
                if (i > 9) break RENDER_RESULT;
                ChanceItemStack chanceItemStack = resultItems.get(i);
                ItemStack stack = chanceItemStack.stack().copy();
                double count = getMax(chanceItemStack.count());
                stack.setCount((int) count);
                int x = i * SLOT_SIZE;
                g.renderItem(stack, x + 1, 51);
                g.renderItemDecorations(Minecraft.getInstance().font, stack, x, 50);
                this.renderTooltip(context, stack, x, 50, mouseX, mouseY);
            }
        }
    }

    public static double getMax(NumberProvider provider) {
        return switch (provider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uniform -> getMax(uniform.max());
            case BinomialDistributionGenerator binomial -> getMax(binomial.n());
            default -> 0;
        };
    }
}
