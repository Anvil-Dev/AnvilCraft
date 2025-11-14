package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BoilingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.CookingRecipe;
import net.minecraft.world.item.Items;

public class CookingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        CookingRecipe.builder()
            .requires(ModItemTags.RESIN)
            .result(ModItems.HARDEND_RESIN)
            .save(provider);

        CookingRecipe.builder()
            .requires(ModItemTags.DOUGH)
            .requires(Items.EGG)
            .requires(Items.SUGAR)
            .result(ModBlocks.CAKE_BASE_BLOCK)
            .save(provider);

        BoilingRecipe.builder()
            .requires(ModItemTags.RESIN)
            .result(Items.SLIME_BALL)
            .save(provider);

        BoilingRecipe.builder()
            .requires(Items.BEEF)
            .requires(Items.BROWN_MUSHROOM)
            .requires(Items.RED_MUSHROOM)
            .requires(Items.BOWL)
            .result(ModFoodItems.BEEF_MUSHROOM_STEW)
            .save(provider);

        CookingRecipe.builder()
            .requires(Items.SPIDER_EYE)
            .requires(Items.PUFFERFISH)
            .requires(Items.POISONOUS_POTATO)
            .requires(Items.LILY_OF_THE_VALLEY)
            .requires(Items.WITHER_ROSE)
            .result(ModFoodItems.UTUSAN)
            .save(provider);
    }
}
