package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MDBulgingRecipeComponent extends MDBaseAnvilRecipeComponent {
    @Getter
    private final List<ItemIngredientPredicate> ingredients;

    @Getter
    private final List<ChanceItemStack> resultItems;

    @Getter
    private final List<BlockState> inputBlockStates;

    @Getter
    private final BulgingRecipe recipe;

    public MDBulgingRecipeComponent(BulgingRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        ingredients = recipe.getInputItems();
        resultItems = recipe.getResultItems();
        inputBlockStates = List.of(
            getInputCauldron(recipe)
        );
        this.recipe = recipe;
    }

    protected BlockState getOutputBlockState() {
        if (resultItems.isEmpty()) {
            return getResultCauldron(recipe);
        }
        return super.getOutputBlockState();
    }

    public static BlockState getInputCauldron(BulgingRecipe recipe) {
        Block material = recipe.getHasCauldron().getFluidCauldron();
        return CauldronUtil.fullState(material);
    }

    static BlockState getResultCauldron(BulgingRecipe recipe) {
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
