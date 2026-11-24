package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class ItemInjectRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        ItemInjectRecipe.builder()
            .requires(Tags.Items.INGOTS_GOLD, 2)
            .inputBlock(Tags.Blocks.NETHERRACKS)
            .resultBlock(Blocks.NETHER_GOLD_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(Tags.Items.INGOTS_GOLD)
            .inputBlock(Blocks.BLACKSTONE)
            .resultBlock(Blocks.GILDED_BLACKSTONE)
            .save(provider);

    }
}
