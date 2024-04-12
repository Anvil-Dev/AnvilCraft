package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CompressRecipesLoader {
    /**
     * 初始化打包配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(3, Items.BONE)
            .spawnItem(Items.BONE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.BONE), AnvilCraftDatagen.has(Items.BONE))
            .save(provider);
    }
}
