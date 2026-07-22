package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MDSolidLiquidRecipeComponent extends MDBaseAnvilRecipeComponent {
    @Getter
    @Nullable
    private final List<ItemIngredientPredicate> ingredients;

    @Getter
    @Nullable
    private final List<ChanceItemStack> resultItems;

    @Getter
    private final List<BlockState> inputBlockStates;

    @Getter
    private final SolidLiquidRecipe recipe;

    public MDSolidLiquidRecipeComponent(SolidLiquidRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        this.ingredients = recipe.getInputItems();
        this.resultItems = recipe.getResultItems();
        this.inputBlockStates = List.of(
            getInputCauldron(recipe)
        );
        this.recipe = recipe;
    }

    protected BlockState getOutputBlockState() {
        if (this.resultItems == null || this.resultItems.isEmpty()) {
            return getResultCauldron(this.recipe);
        }
        return super.getOutputBlockState();
    }

    public static BlockState getInputCauldron(SolidLiquidRecipe recipe) {
        Block material = recipe.getHasCauldron().getFluidCauldron();
        return CauldronUtil.fullState(material);
    }

    static BlockState getResultCauldron(SolidLiquidRecipe recipe) {
        Block result = recipe.getHasCauldron().getTransformCauldron();
        if (recipe.isConsumeFluid()) {
            return CauldronUtil.getStateFromContentAndLevel(result, CauldronUtil.maxLevel(result) - 1);
        } else if (recipe.isProduceFluid()) {
            return CauldronUtil.getStateFromContentAndLevel(result, 1);
        } else {
            return CauldronUtil.fullState(result);
        }
    }
}
