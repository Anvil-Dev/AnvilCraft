package dev.dubhe.anvilcraft.integration.patchouli.page.anvilitem;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;

public class PageItemCrush extends PageAnvilItemProcess<ItemCrushRecipe> {
    public PageItemCrush() {
        super(
            ModRecipeTypes.ITEM_CRUSH_TYPE.get(),
            ItemCrushRecipe::getInputItems,
            ItemCrushRecipe::getResultItems,
            recipe -> ModBlocks.CRUSHING_TABLE.getDefaultState(),
            null);
    }
}
