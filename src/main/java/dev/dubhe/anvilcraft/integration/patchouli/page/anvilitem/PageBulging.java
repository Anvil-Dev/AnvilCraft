package dev.dubhe.anvilcraft.integration.patchouli.page.anvilitem;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PageBulging extends PageAnvilItemProcess<BulgingRecipe> {
    public PageBulging() {
        super(
            ModRecipeTypes.BULGING_TYPE.get(),
            BulgingRecipe::getInputItems,
            BulgingRecipe::getResultItems,
            PageBulging::getInputCauldron,
            null
        );
    }

    @Override
    protected void drawExtra(GuiGraphics graphics, BulgingRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second) {
        // 如果产生液体，渲染装有液体的锅
        if (!recipe.getResultItems().isEmpty()) return;
        RenderSupport.renderBlock(
            graphics,
            getResultCauldron(recipe),
            recipeX + 90,
            recipeY + 29,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );
    }

    static BlockState getInputCauldron(BulgingRecipe recipe) {
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
