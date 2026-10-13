package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import lombok.Getter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MDBlockSmearRecipeComponent extends MDBaseAnvilRecipeComponent {

    private final List<BlockStatePredicate> inputBlocks;
    @Getter
    private final BlockState outputBlockState;

    public MDBlockSmearRecipeComponent(BlockSmearRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        this.inputBlocks = recipe.getInputBlocks();
        this.outputBlockState = recipe.getFirstResultBlock().state();
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        super.renderRecipe(context, mouseX, mouseY);
        for (int i = 0; i < this.inputBlocks.size(); i++) {
            List<BlockState> states = this.inputBlocks.get(i).constructStatesForRender();
            if (!states.isEmpty()) {
                BlockState blockState = states.get(RecipeUtil.getDisplayIndex(states.size()));
                AgeratumUtil.renderBlock(
                    context,
                    blockState,
                    mouseX,
                    mouseY,
                    INPUT_BLOCK_X,
                    AgeratumUtil.getRenderY(BLOCK_Y, i),
                    (this.inputBlocks.size() - i) * 10
                );
            }
        }
    }
}
