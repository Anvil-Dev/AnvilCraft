package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import lombok.Getter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MDItemInjectRecipeComponent extends MDBaseAnvilRecipeComponent {
    @Getter
    @Nullable
    private final List<ItemIngredientPredicate> ingredients;

    @Getter
    @Nullable
    private final List<ChanceItemStack> resultItems;

    private final BlockStatePredicate inputBlock;

    @Getter
    private final BlockState outputBlockState;

    public MDItemInjectRecipeComponent(ItemInjectRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        ingredients = recipe.getInputItems();
        resultItems = recipe.getResultItems();
        inputBlock = recipe.getFirstInputBlock();
        outputBlockState = recipe.getFirstResultBlock().state();
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        super.renderRecipe(context, mouseX, mouseY);
        List<BlockState> states = inputBlock.constructStatesForRender();
        if (!states.isEmpty()) {
            BlockState blockState = states.get(RecipeUtil.getDisplayIndex(states.size()));
            AgeratumUtil.renderBlock(context, blockState, mouseX, mouseY, INPUT_BLOCK_X, BLOCK_Y, 0);
        }
    }
}
