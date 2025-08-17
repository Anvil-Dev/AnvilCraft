package dev.dubhe.anvilcraft.integration.patchouli.page;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PageBulging extends PageAnvilItemProcess<BulgingRecipe> {
    public PageBulging() {
        super(
            ModRecipeTypes.BULGING_TYPE.get(),
            BulgingRecipe::getInputItems,
            BulgingRecipe::getResultItems,
            recipe -> CauldronUtil.fullState(Blocks.WATER_CAULDRON),
            null);
    }

    @Override
    protected void drawExtra(GuiGraphics graphics, BulgingRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second) {
        if (!recipe.getResultItems().isEmpty() || getCauldron(recipe) == null) return;
        RenderHelper.renderBlock(
            graphics, getCauldron(recipe),
            recipeX + 90, recipeY + 29, 10,
            12,
            RenderHelper.SINGLE_BLOCK);
    }

    static BlockState getCauldron(BulgingRecipe recipe) {
        if (recipe.isProduceFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        } else {
            return CauldronUtil.fullState(BuiltInRegistries.BLOCK.get(recipe.getHasCauldron().getFluid().withSuffix("_cauldron")));
        }
    }
}
