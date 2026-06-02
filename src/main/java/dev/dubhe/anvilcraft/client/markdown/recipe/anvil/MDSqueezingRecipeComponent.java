package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import lombok.Getter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MDSqueezingRecipeComponent extends MDBaseAnvilRecipeComponent {

    private final List<BlockStatePredicate> inputBlocks;
    @Getter
    private final BlockState outputBlockState;

    public MDSqueezingRecipeComponent(SqueezingRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        inputBlocks = recipe.getInputBlocks();
        outputBlockState = recipe.getFirstResultBlock().state();
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        super.renderRecipe(context, mouseX, mouseY);
        List<BlockState> states = inputBlocks.getFirst().constructStatesForRender();
        if (!states.isEmpty()) {
            BlockState blockState = states.get(RecipeUtil.getDisplayIndex(states.size()));
            AgeratumUtil.renderBlock(context, blockState, mouseX, mouseY, INPUT_BLOCK_X, BLOCK_Y, 10);
        }
        int y = AgeratumUtil.getRenderY(BLOCK_Y, 1);
        AgeratumUtil.renderBlock(context, Blocks.CAULDRON.defaultBlockState(), mouseX, mouseY, INPUT_BLOCK_X, y, 0);
    }
}
