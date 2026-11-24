package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.FastCookingRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class FastCookingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        FastCookingRecipe.builder()
            .requires(ModItemTags.RESIN)
            .result(ModItems.HARDENED_RESIN)
            .save(provider);

        FastCookingRecipe.builder()
            .requires(ModItemTags.DOUGH)
            .requires(Tags.Items.EGGS)
            .requires(Items.SUGAR)
            .result(ModBlocks.CAKE_BASE_BLOCK)
            .save(provider);

        FastCookingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(ModItemTags.RESIN)
            .result(Items.SLIME_BALL)
            .save(provider);

        FastCookingRecipe.builder()
            .cauldron(Blocks.WATER_CAULDRON)
            .requires(ModItemTags.RAW_BEEF)
            .requires(Items.BROWN_MUSHROOM)
            .requires(Items.RED_MUSHROOM)
            .requires(Items.BOWL)
            .result(ModFoodItems.BEEF_MUSHROOM_STEW)
            .save(provider);

        FastCookingRecipe.builder()
            .requires(Items.MUD)
            .result(Items.CLAY)
            .save(provider);

    }
}
