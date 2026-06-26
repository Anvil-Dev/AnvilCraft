package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockProcessingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import lombok.Getter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MDBlockProcessingRecipeComponent extends MDBaseAnvilRecipeComponent {

    private final List<BlockStatePredicate> inputBlocks;
    @Getter
    private final BlockState outputBlockState;

    public MDBlockProcessingRecipeComponent(BlockProcessingRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        inputBlocks = recipe.getInputBlocks();
        outputBlockState = recipe.getFirstResultBlock().state();
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        super.renderRecipe(context, mouseX, mouseY);
        for (int i = 0; i < inputBlocks.size(); i++) {
            List<BlockState> states = inputBlocks.get(i).constructStatesForRender();
            if (!states.isEmpty()) {
                BlockState blockState = states.get(RecipeUtil.getDisplayIndex(states.size()));
                AgeratumUtil.renderBlock(
                    context,
                    blockState,
                    mouseX,
                    mouseY,
                    INPUT_BLOCK_X,
                    AgeratumUtil.getRenderY(BLOCK_Y, i),
                    (inputBlocks.size() - i) * 10
                );
            }
        }
    }
}