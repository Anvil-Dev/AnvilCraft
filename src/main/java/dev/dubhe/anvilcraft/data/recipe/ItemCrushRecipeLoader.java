package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.ItemCrushRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class ItemCrushRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ItemCrushRecipe.builder()
                .requires(Items.WET_SPONGE)
                .result(new ItemStack(ModItems.SPONGE_GEMMULE.asItem(), 4))
                .save(provider);
        ItemCrushRecipe.builder()
                .requires(Items.HONEY_BLOCK)
                .requires(Items.GLASS_BOTTLE, 4)
                .result(new ItemStack(Items.HONEY_BOTTLE, 4))
                .save(provider);
    }
}
