package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class JewelCraftingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        JewelCraftingRecipe.builder()
            .requires(Items.EXPERIENCE_BOTTLE, 16)
            .requires(Items.GOLD_BLOCK, 8)
            .requires(Items.GOLDEN_APPLE)
            .result(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE))
            .save(provider);

        JewelCraftingRecipe.builder()
            .requires(Items.EMERALD_BLOCK, 2)
            .requires(Items.ENCHANTED_GOLDEN_APPLE)
            .requires(ModBlocks.ROYAL_STEEL_BLOCK)
            .result(new ItemStack(Items.TOTEM_OF_UNDYING))
            .save(provider);

        JewelCraftingRecipe.builder()
            .requires(Items.PHANTOM_MEMBRANE, 8)
            .requires(Items.BAMBOO, 4)
            .requires(ModItemTags.TITANIUM_INGOTS)
            .result(new ItemStack(Items.ELYTRA))
            .save(provider);
    }
}
