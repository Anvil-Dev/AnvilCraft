package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.SuperHeatingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class SuperHeatingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        SuperHeatingRecipe.builder()
                .requires(Items.COAL_BLOCK, 16)
                .result(new ItemStack(Items.DIAMOND_BLOCK))
                .save(provider);

        SuperHeatingRecipe.builder()
                .blockResult(ModBlocks.LAVA_CAULDRON.get())
                .requires(Items.COBBLESTONE, 4)
                .requires(ModItems.LIME_POWDER)
                .save(provider);
    }
}
