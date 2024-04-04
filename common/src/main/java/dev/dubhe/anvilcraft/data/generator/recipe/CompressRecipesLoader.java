package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CompressRecipesLoader {

    public static void init(RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.FRUIT_OF_THE_SEA.get()), AnvilCraftDatagen.has(ModItems.FRUIT_OF_THE_SEA))
                .hasItemIngredient(ModItems.FRUIT_OF_THE_SEA)
                .spawnItem(ModItems.KERNEL_OF_THE_SEA)
                .save(provider);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(Items.PRISMARINE_SHARD)
                .hasItemIngredient(ModItems.TEAR_OF_THE_SEA)
                .spawnItem(ModItems.BLADE_OF_THE_SEA)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.PRISMARINE_SHARD), AnvilCraftDatagen.has(Items.PRISMARINE_SHARD))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TEAR_OF_THE_SEA.get()), AnvilCraftDatagen.has(ModItems.TEAR_OF_THE_SEA))
                .save(provider);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(3,Items.BONE)
                .spawnItem(Items.BONE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.BONE), AnvilCraftDatagen.has(Items.BONE))
                .save(provider);
    }
}
