package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.FastCookingRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class FastCookingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        FastCookingRecipe.builder()
            .requires(ModItemTags.RESIN)
            .result(ModItems.HARDEND_RESIN)
            .save(provider);

        FastCookingRecipe.builder()
            .requires(ModItemTags.DOUGH)
            .requires(Items.EGG)
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
            .requires(Items.BEEF)
            .requires(Items.BROWN_MUSHROOM)
            .requires(Items.RED_MUSHROOM)
            .requires(Items.BOWL)
            .result(ModFoodItems.BEEF_MUSHROOM_STEW)
            .save(provider);

        FastCookingRecipe.builder()
            .requires(Items.SPIDER_EYE)
            .requires(Items.PUFFERFISH)
            .requires(Items.POISONOUS_POTATO)
            .requires(Items.LILY_OF_THE_VALLEY)
            .requires(Items.WITHER_ROSE)
            .result(ModFoodItems.UTUSAN)
            .save(provider);
    }
}
