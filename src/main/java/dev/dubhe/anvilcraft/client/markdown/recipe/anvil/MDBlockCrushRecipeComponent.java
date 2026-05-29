package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import lombok.Getter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MDBlockCrushRecipeComponent extends MDBaseAnvilRecipeComponent {
    private final List<BlockStatePredicate> inputBlocks;
    @Getter
    private final BlockState outputBlockState;

    public MDBlockCrushRecipeComponent(BlockCrushRecipe recipe, boolean enableAlignCenter) {
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
                AgeratumUtil.renderBlock(context, blockState, mouseX, mouseY, INPUT_BLOCK_X, BLOCK_Y + i * AgeratumUtil.BLOCK_SIZE, 0);
            }
        }
    }
}
