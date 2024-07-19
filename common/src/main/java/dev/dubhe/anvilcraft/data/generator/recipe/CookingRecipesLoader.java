package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.block.HasBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;

public class CookingRecipesLoader {
    /**
     * 初始化烹饪配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        boil(ModItems.BEEF_MUSHROOM_STEW_RAW.get(), 1, ModItems.BEEF_MUSHROOM_STEW.get(), 1, provider);
        boil(ModItems.RESIN.get(), 1, Items.SLIME_BALL, 1, provider);
        cook(ModItems.UTUSAN_RAW.get(), 1, ModItems.UTUSAN.get(), 1, provider);
        cook(ModItems.RESIN.get(), 1, ModItems.HARDEND_RESIN.get(), 1, provider);

        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
            .type(AnvilRecipeType.COOKING)
            .hasBlock(Blocks.CAULDRON)
            .hasBlock(new Vec3(0.0, -2.0, 0.0),
                new HasBlock.ModBlockPredicate().block(BlockTags.CAMPFIRES).property(CampfireBlock.LIT, true)
            )
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), ModItems.DOUGH)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.EGG)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.SUGAR)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), ModBlocks.CAKE_BASE_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH), AnvilCraftDatagen.has(ModItems.DOUGH))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.EGG), AnvilCraftDatagen.has(Items.EGG))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUGAR), AnvilCraftDatagen.has(Items.SUGAR))
            .save(provider, "cook/cake_base_block");
    }

    /**
     * 简单烹饪配方
     *
     * @param item     物品
     * @param count    数量
     * @param item1    产物
     * @param count1   数量
     * @param provider 提供器
     */
    public static void cook(Item item, int count, Item item1, int count1, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
            .type(AnvilRecipeType.COOKING)
            .hasBlock(Blocks.CAULDRON)
            .hasBlock(new Vec3(0.0, -2.0, 0.0),
                new HasBlock.ModBlockPredicate().block(BlockTags.CAMPFIRES).property(CampfireBlock.LIT, true)
            )
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, item)
            .spawnItem(item1, count1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, "cook/" + BuiltInRegistries.ITEM.getKey(item1).getPath());
    }

    /**
     * 简单炖煮配方
     *
     * @param item     物品
     * @param count    数量
     * @param item1    产物
     * @param count1   数量
     * @param provider 提供器
     */
    public static void boil(Item item, int count, Item item1, int count1, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
            .type(AnvilRecipeType.COOKING)
            .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), Blocks.WATER_CAULDRON)
            .hasBlock(
                new Vec3(0.0, -2.0, 0.0),
                new HasBlock.ModBlockPredicate().block(BlockTags.CAMPFIRES).property(CampfireBlock.LIT, true)
            )
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, item)
            .spawnItem(item1, count1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, "boil/" + BuiltInRegistries.ITEM.getKey(item1).getPath());
    }
}
