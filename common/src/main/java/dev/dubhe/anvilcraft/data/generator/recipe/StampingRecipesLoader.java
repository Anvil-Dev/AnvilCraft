package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class StampingRecipesLoader {


    public static void init(RegistrateRecipeProvider provider)  {
        stamping(Items.IRON_INGOT, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, provider);
        stamping(Items.GOLD_INGOT, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, provider);
        stamping(Items.SUGAR_CANE, Items.PAPER, provider);
        stamping(Items.SNOWBALL, Items.SNOW, provider);
        stamping(ModItems.PULP.get(), Items.PAPER, provider);
        stamping(Items.MILK_BUCKET, ModItems.CREAM.get(), provider);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
                .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), Items.COCOA_BEANS)
                .spawnItem(new Vec3(0.0, -0.75, 0.0), ModItems.COCOA_BUTTER)
                .spawnItem(new Vec3(0.0, -0.75, 0.0), ModItems.COCOA_POWDER)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.COCOA_BEANS), AnvilCraftDatagen.has(Items.COCOA_BEANS))
                .save(provider, AnvilCraft.of("stamping/cocoa"));
    }

    public static void stamping(Item item, Item item1, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
                .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), item)
                .spawnItem(new Vec3(0.0, -0.75, 0.0), item1)
                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
                .save(provider, AnvilCraft.of("stamping/" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_2_" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
