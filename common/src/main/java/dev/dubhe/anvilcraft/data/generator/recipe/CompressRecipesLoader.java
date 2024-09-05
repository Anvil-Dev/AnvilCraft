package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.anvilcraft.lib.data.provider.RegistratorRecipeProvider;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
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
    public static void init(RegistratorRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.COMPRESS)
            .hasBlock(Blocks.CAULDRON)
            .hasNotBlock(new Vec3(0.0, -2.0, 0.0), ModBlockTags.UNDER_CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 3, Items.BONE)
            .spawnItem(Items.BONE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.BONE), AnvilCraftDatagen.has(Items.BONE))
            .save(provider);

        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.GENERIC)
            .hasBlock(Blocks.CAULDRON)
            .hasNotBlock(new Vec3(0.0, -2.0, 0.0), ModBlockTags.UNDER_CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 4, ModItems.CREAM)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.SUGAR)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), ModBlocks.CREAM_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CREAM), AnvilCraftDatagen.has(ModItems.CREAM))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUGAR), AnvilCraftDatagen.has(Items.SUGAR))
            .save(provider);

        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.GENERIC)
            .hasBlock(Blocks.CAULDRON)
            .hasNotBlock(new Vec3(0.0, -2.0, 0.0), ModBlockTags.UNDER_CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 4, ModItems.CREAM)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.SUGAR)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.SWEET_BERRIES)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), ModBlocks.BERRY_CREAM_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CREAM), AnvilCraftDatagen.has(ModItems.CREAM))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUGAR), AnvilCraftDatagen.has(Items.SUGAR))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SWEET_BERRIES), AnvilCraftDatagen.has(Items.SWEET_BERRIES))
            .save(provider);

        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.GENERIC)
            .hasBlock(Blocks.CAULDRON)
            .hasNotBlock(new Vec3(0.0, -2.0, 0.0), ModBlockTags.UNDER_CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 4, ModItems.CREAM)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.SUGAR)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItems.CHOCOLATE)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), ModBlocks.CHOCOLATE_CREAM_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CREAM), AnvilCraftDatagen.has(ModItems.CREAM))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUGAR), AnvilCraftDatagen.has(Items.SUGAR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CHOCOLATE), AnvilCraftDatagen.has(ModItems.CHOCOLATE))
            .save(provider);
    }
}
