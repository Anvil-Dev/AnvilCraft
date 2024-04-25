package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class BulgingAndCrystallizeRecipesLoader {
    /**
     * 初始化彭发和晶化配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        bulging(Items.DIRT, Items.CLAY, provider);
        bulging(Items.CRIMSON_FUNGUS, Items.NETHER_WART_BLOCK, provider);
        bulging(Items.WARPED_FUNGUS, Items.WARPED_WART_BLOCK, provider);
        bulging(Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE, provider);
        bulging(Items.BRAIN_CORAL, Items.BRAIN_CORAL_BLOCK, provider);
        bulging(Items.BUBBLE_CORAL, Items.BUBBLE_CORAL_BLOCK, provider);
        bulging(Items.FIRE_CORAL, Items.FIRE_CORAL_BLOCK, provider);
        bulging(Items.HORN_CORAL, Items.HORN_CORAL_BLOCK, provider);
        bulging(Items.TUBE_CORAL, Items.TUBE_CORAL_BLOCK, provider);
        bulging(ModItems.WOOD_FIBER.get(), ModItems.PULP.get(), provider);
        bulging(ModItems.SPONGE_GEMMULE.get(), Items.WET_SPONGE, provider);
        crystallize(ModItems.SEA_HEART_SHELL_SHARD.get(), ModItems.PRISMARINE_CLUSTER.get(), provider);
    }

    /**
     * 膨发配方
     *
     * @param item     原料
     * @param item1    产物
     * @param provider 提供器
     */
    public static void bulging(Item item, Item item1, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(item1)
            .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), Blocks.WATER_CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("bulging/" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }

    private static void crystallize(Item item, Item item1, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(item1)
            .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), Blocks.POWDER_SNOW_CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("bulging/" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
