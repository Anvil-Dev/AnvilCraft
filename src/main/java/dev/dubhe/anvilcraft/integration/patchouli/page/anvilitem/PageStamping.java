package dev.dubhe.anvilcraft.integration.patchouli.page.anvilitem;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;

public class PageStamping extends PageAnvilItemProcess<StampingRecipe> {
    public PageStamping() {
        super(
            ModRecipeTypes.STAMPING_TYPE.get(),
            StampingRecipe::getInputItems,
            StampingRecipe::getResultItems,
            recipe -> ModBlocks.STAMPING_PLATFORM.getDefaultState(),
            null);
    }
}
