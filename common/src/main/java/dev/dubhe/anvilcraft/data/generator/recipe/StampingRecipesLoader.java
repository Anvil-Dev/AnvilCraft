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
    /**
     * 初始化配方
     *
     * @param provider 配方提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
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
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), Items.HEART_OF_THE_SEA)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), ModItems.SEA_HEART_SHELL_SHARD, 3)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.5, ModItems.SEA_HEART_SHELL_SHARD)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.5, ModItems.SEA_HEART_SHELL_SHARD)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), ModItems.SAPPHIRE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.HEART_OF_THE_SEA),
                AnvilCraftDatagen.has(Items.HEART_OF_THE_SEA))
            .save(provider, AnvilCraft.of("stamping/sea_heart_shell_shard"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), ModItems.PRISMARINE_CLUSTER)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.15, ModItems.PRISMARINE_BLADE)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), Items.PRISMARINE_CRYSTALS, 2)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.5, Items.PRISMARINE_CRYSTALS)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), Items.PRISMARINE_SHARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PRISMARINE_CLUSTER.get()),
                AnvilCraftDatagen.has(ModItems.PRISMARINE_CLUSTER))
            .save(provider, AnvilCraft.of("stamping/prismarine_blade"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), ModItems.GEODE)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), Items.AMETHYST_SHARD, 2)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.25, ModItems.TOPAZ)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.25, ModItems.SAPPHIRE)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.25, ModItems.RUBY)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PRISMARINE_CLUSTER.get()),
                AnvilCraftDatagen.has(ModItems.PRISMARINE_CLUSTER))
            .save(provider, AnvilCraft.of("stamping/geode_2_jewel"));
    }

    /**
     * 生成简单冲压配方
     *
     * @param item     原料
     * @param item1    产物
     * @param provider 配方提供器
     */
    public static void stamping(Item item, Item item1, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), item)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), item1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("stamping/" + BuiltInRegistries.ITEM.getKey(item).getPath()
                + "_2_" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
