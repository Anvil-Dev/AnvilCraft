package dev.dubhe.anvilcraft.integration.patchouli.page.anvilitem;

import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BoilingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.world.level.block.Blocks;

public class PageBoiling extends PageAnvilItemProcess<BoilingRecipe> {
    public PageBoiling() {
        super(
            ModRecipeTypes.BOILING_TYPE.get(),
            BoilingRecipe::getInputItems,
            BoilingRecipe::getResultItems,
            recipe -> CauldronUtil.fullState(Blocks.WATER_CAULDRON),
            recipe -> Blocks.CAMPFIRE.defaultBlockState());
    }
}
