package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import lombok.Getter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MDBlockCompressRecipeComponent extends MDBaseAnvilRecipeComponent {

    private final List<BlockStatePredicate> inputBlocks;
    @Getter
    private final BlockState outputBlockState;

    public MDBlockCompressRecipeComponent(BlockCompressRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        this.inputBlocks = recipe.getInputBlocks();
        this.outputBlockState = recipe.getFirstResultBlock().state();
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        super.extractRecipeRenderState(context, mouseX, mouseY);
        for (int i = 0; i < this.inputBlocks.size(); i++) {
            List<BlockState> states = this.inputBlocks.get(i).constructStatesForRender();
            if (!states.isEmpty()) {
                BlockState blockState = states.get(RecipeUtil.getDisplayIndex(states.size()));
                AgeratumUtil.renderBlock(context, blockState, mouseX, mouseY, MDBaseAnvilRecipeComponent.INPUT_BLOCK_X, AgeratumUtil.getRenderY(
                    MDBaseAnvilRecipeComponent.BLOCK_Y, i));
            }
        }
    }
}
