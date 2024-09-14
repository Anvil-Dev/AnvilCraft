package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.BoilingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.CookingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class CookingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        CookingRecipe.builder()
                .requires(ModItems.UTUSAN_RAW)
                .result(new ItemStack(ModItems.UTUSAN.asItem()))
                .save(provider);

        CookingRecipe.builder()
                .requires(ModItems.RESIN)
                .result(new ItemStack(ModItems.HARDEND_RESIN.asItem()))
                .save(provider);

        BoilingRecipe.builder()
                .requires(ModItems.BEEF_MUSHROOM_STEW_RAW)
                .result(new ItemStack(ModItems.BEEF_MUSHROOM_STEW.asItem()))
                .save(provider);

        BoilingRecipe.builder()
                .requires(ModItems.RESIN)
                .result(new ItemStack(Items.SLIME_BALL))
                .save(provider);
    }
}
