package dev.dubhe.anvilcraft.integration.patchouli.page.anvilitem;

import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (HasCauldron.isNotEmpty(hasCauldron.transform())) {
            BlockState cauldron = CauldronUtil.fullState(hasCauldron.getTransformCauldron());
            RenderSupport.renderBlock(graphics, cauldron, recipeX + 90, recipeY + 29, 10, 12, RenderSupport.SINGLE_BLOCK);
        }
    }
}
