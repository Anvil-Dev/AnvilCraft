package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ItemInjectRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        ItemInjectRecipe.builder()
            .requires(Items.GOLD_INGOT, 2)
            .inputBlock(Blocks.NETHERRACK)
            .resultBlock(Blocks.NETHER_GOLD_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(Items.GOLD_INGOT)
            .inputBlock(Blocks.BLACKSTONE)
            .resultBlock(Blocks.GILDED_BLACKSTONE)
            .save(provider);

    }
}
