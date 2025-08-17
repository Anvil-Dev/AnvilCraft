package dev.dubhe.anvilcraft.integration.patchouli.page;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import net.minecraft.world.level.block.Blocks;

public class PageUnpack extends PageAnvilItemProcess<UnpackRecipe> {
    public PageUnpack() {
        super(
            ModRecipeTypes.UNPACK_TYPE.get(),
            UnpackRecipe::getInputItems,
            UnpackRecipe::getResultItems,
            recipe -> Blocks.IRON_TRAPDOOR.defaultBlockState(),
            null);
    }
}
