package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class CompressRecipesLoader {
    /**
     * 初始化打包配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.CAULDRON)
            .hasNotBlock(new Vec3(0.0, -2.0, 0.0), ModBlockTags.UNDER_CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 3, Items.BONE)
            .spawnItem(Items.BONE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.BONE), AnvilCraftDatagen.has(Items.BONE))
            .save(provider);
    }
}
