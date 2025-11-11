package dev.dubhe.anvilcraft.integration.patchouli.page.anvilitem;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PageBulging extends PageAnvilItemProcess<BulgingRecipe> {
    public PageBulging() {
        super(
            ModRecipeTypes.BULGING_TYPE.get(),
            BulgingRecipe::getInputItems,
            BulgingRecipe::getResultItems,
            recipe -> CauldronUtil.fullState(Blocks.WATER_CAULDRON),
            null
        );
    }

    @Override
    protected void drawExtra(GuiGraphics graphics, BulgingRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second) {
        if (!recipe.getResultItems().isEmpty()) return;
        RenderSupport.renderBlock(
            graphics,
            getCauldron(recipe),
            recipeX + 90,
            recipeY + 29,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );
    }

    static BlockState getCauldron(BulgingRecipe recipe) {
        if (recipe.isProduceFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        } else {
            return recipe.getHasCauldron().getTransformCauldron().defaultBlockState();
        }
    }
}
