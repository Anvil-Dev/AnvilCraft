package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

public class SmithingRecipesLoader {
    public static void init(RegistrateRecipeProvider provider) {
        SmithingTransformRecipeBuilder
                .smithing(
                        Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItems.ANVIL_HAMMER),
                        Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                        RecipeCategory.TOOLS,
                        ModItems.ROYAL_ANVIL_HAMMER.asItem()
                ).unlocks(
                        AnvilCraftDatagen.hasItem(ModItems.ANVIL_HAMMER),
                        AnvilCraftDatagen.has(ModItems.ANVIL_HAMMER)
                ).unlocks(
                        AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                        AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
                ).save(
                        provider,
                        new ResourceLocation("anvilcraft", "anvil_hammer_upgrade")
                );
    }
}
