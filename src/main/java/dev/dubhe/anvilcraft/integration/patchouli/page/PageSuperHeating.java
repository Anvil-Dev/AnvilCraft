package dev.dubhe.anvilcraft.integration.patchouli.page;

import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PageSuperHeating extends PageAnvilItemProcess<SuperHeatingRecipe> {
    public PageSuperHeating() {
        super(
            ModRecipeTypes.SUPER_HEATING_TYPE.get(),
            SuperHeatingRecipe::getInputItems,
            SuperHeatingRecipe::getResultItems,
            recipe -> Blocks.CAULDRON.defaultBlockState(),
            recipe -> ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false)
        );
    }

    @Override
    protected void drawExtra(
        GuiGraphics graphics, SuperHeatingRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY,
        boolean second
    ) {
        List<ChanceBlockState> blockResults = recipe.getResultBlocks();
        if (!recipe.getResultItems().isEmpty() || blockResults.isEmpty()) return;
        BlockState state = blockResults.get((parent.ticksInBook / 20) % blockResults.size()).getState();
        RenderHelper.renderBlock(
            graphics,
            state.getBlock() instanceof CauldronBlock ? CauldronUtil.fullState(state.getBlock()) : state,
            recipeX + 85, recipeY + 29, 10,
            12,
            RenderHelper.SINGLE_BLOCK);
    }
}
