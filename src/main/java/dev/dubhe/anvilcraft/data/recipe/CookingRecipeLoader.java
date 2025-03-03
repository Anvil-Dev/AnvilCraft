package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.BoilingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.CookingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CookingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        CookingRecipe.builder()
            .requires(ModItems.RESIN)
            .result(new ItemStack(ModItems.HARDEND_RESIN.asItem()))
            .save(provider);

        CookingRecipe.builder()
            .requires(ModItems.DOUGH)
            .requires(Items.EGG)
            .requires(Items.SUGAR)
            .result(new ItemStack(ModBlocks.CAKE_BASE_BLOCK))
            .save(provider);

        BoilingRecipe.builder()
            .requires(ModItems.RESIN)
            .result(new ItemStack(Items.SLIME_BALL))
            .save(provider);

        BoilingRecipe.builder()
            .requires(Items.BEEF)
            .requires(Items.BROWN_MUSHROOM)
            .requires(Items.RED_MUSHROOM)
            .requires(Items.BOWL)
            .result(new ItemStack(ModItems.BEEF_MUSHROOM_STEW.asItem()))
            .save(provider);

        CookingRecipe.builder()
            .requires(Items.SPIDER_EYE)
            .requires(Items.PUFFERFISH)
            .requires(Items.POISONOUS_POTATO)
            .requires(Items.LILY_OF_THE_VALLEY)
            .requires(Items.WITHER_ROSE)
            .result(new ItemStack(ModItems.UTUSAN.asItem()))
            .save(provider);
    }
}
