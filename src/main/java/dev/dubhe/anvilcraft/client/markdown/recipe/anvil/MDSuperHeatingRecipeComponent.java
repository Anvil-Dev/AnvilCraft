package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MDSuperHeatingRecipeComponent extends MDBaseAnvilRecipeComponent {
    @Getter
    @Nullable
    private final List<ItemIngredientPredicate> ingredients;

    @Getter
    @Nullable
    private final List<ChanceItemStack> resultItems;

    @Getter
    private final List<BlockState> inputBlockStates;

    @Nullable
    private final BlockState outputCauldron;

    public MDSuperHeatingRecipeComponent(SuperHeatingRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        ingredients = recipe.getInputItems();
        resultItems = recipe.getResultItems();
        inputBlockStates = List.of(
            getInputCauldron(recipe),
            ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false)
        );
        outputCauldron = !resultItems.isEmpty() ? null : getResultCauldron(recipe);
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        super.renderRecipe(context, mouseX, mouseY);
        if (outputCauldron != null) {
            AgeratumUtil.renderBlock(context, outputCauldron, mouseX, mouseY, 90, 29, 10);
        }
    }

    public static BlockState getInputCauldron(SuperHeatingRecipe recipe) {
        if (recipe.isProduceFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        } else {
            return recipe.getHasCauldron().getTransformCauldron().defaultBlockState();
        }
    }

    static BlockState getResultCauldron(SuperHeatingRecipe recipe) {
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
