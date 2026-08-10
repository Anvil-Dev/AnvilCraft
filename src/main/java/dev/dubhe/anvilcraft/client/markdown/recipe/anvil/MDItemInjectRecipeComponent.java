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
import org.jspecify.annotations.Nullable;

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
        this.ingredients = recipe.getInputItems();
        this.resultItems = recipe.getResultItems();
        this.inputBlock = recipe.getFirstInputBlock();
        this.outputBlockState = recipe.getFirstResultBlock().state();
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        super.extractRecipeRenderState(context, mouseX, mouseY);
        List<BlockState> states = this.inputBlock.constructStatesForRender();
        if (!states.isEmpty()) {
            BlockState blockState = states.get(RecipeUtil.getDisplayIndex(states.size()));
            AgeratumUtil.renderBlock(
                context, blockState, mouseX, mouseY, MDBaseAnvilRecipeComponent.INPUT_BLOCK_X, MDBaseAnvilRecipeComponent.BLOCK_Y);
        }
    }
}
