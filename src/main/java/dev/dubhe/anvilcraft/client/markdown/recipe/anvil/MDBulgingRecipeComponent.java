package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MDBulgingRecipeComponent extends MDBaseAnvilRecipeComponent {
    @Getter
    @Nullable
    private final List<ItemIngredientPredicate> ingredients;

    @Getter
    @Nullable
    private final List<ChanceItemStack> resultItems;

    @Getter
    private final List<BlockState> inputBlockStates;

    @Getter
    private final BlockState outputBlockState;

    public MDBulgingRecipeComponent(BulgingRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        ingredients = recipe.getInputItems();
        resultItems = recipe.getResultItems();
        inputBlockStates = List.of(
            getInputCauldron(recipe),
            Blocks.SCAFFOLDING.defaultBlockState()
        );
        outputBlockState = !resultItems.isEmpty() ? Blocks.AIR.defaultBlockState() : getResultCauldron(recipe);
    }

    public static BlockState getInputCauldron(BulgingRecipe recipe) {
        if (recipe.isFromWater()) {
            return CauldronUtil.fullState(Blocks.WATER_CAULDRON);
        } else if (recipe.isProduceFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        } else {
            return recipe.getHasCauldron().getTransformCauldron().defaultBlockState();
        }
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
