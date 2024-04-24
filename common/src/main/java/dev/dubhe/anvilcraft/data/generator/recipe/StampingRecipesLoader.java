package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.Vec3;

public class StampingRecipesLoader {
    private static RegistrateRecipeProvider provider = null;

    /**
     * 初始化配方
     *
     * @param provider 配方提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        StampingRecipesLoader.provider = provider;
        stamping(Items.IRON_INGOT, Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
        stamping(Items.GOLD_INGOT, Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        stamping(Items.SNOWBALL, Items.SNOW);
        stamping(ModItems.PULP.get(), Items.PAPER);
        stamping(Items.MILK_BUCKET, ModItems.CREAM.get());
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), Items.SUGAR_CANE)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), Items.PAPER)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.25, Items.SUGAR)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUGAR_CANE), AnvilCraftDatagen.has(Items.SUGAR_CANE))
            .save(provider, AnvilCraft.of("stamping/paper"));
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
            .addOutcomes(
                new SelectOne()
                    .add(new SpawnItem(new Vec3(0.0, -0.75, 0.0), 1.0, new ItemStack(ModItems.TOPAZ)))
                    .add(new SpawnItem(new Vec3(0.0, -0.75, 0.0), 1.0, new ItemStack(ModItems.SAPPHIRE)))
                    .add(new SpawnItem(new Vec3(0.0, -0.75, 0.0), 1.0, new ItemStack(ModItems.RUBY)))
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PRISMARINE_CLUSTER.get()),
                AnvilCraftDatagen.has(ModItems.PRISMARINE_CLUSTER))
            .save(provider, AnvilCraft.of("stamping/geode_2_jewel"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), ItemTags.LOGS)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), ModItems.WOOD_FIBER, 2)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.25, ModItems.RESIN)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ItemTags.LOGS))
            .save(provider, AnvilCraft.of("stamping/resin"));

        reclaim(Items.CHAINMAIL_HELMET, Items.CHAIN);
        reclaim(Items.CHAINMAIL_CHESTPLATE, Items.CHAIN);
        reclaim(Items.CHAINMAIL_LEGGINGS, Items.CHAIN);
        reclaim(Items.CHAINMAIL_BOOTS, Items.CHAIN);

        reclaim(Items.IRON_HELMET, Items.IRON_INGOT);
        reclaim(Items.IRON_CHESTPLATE, Items.IRON_INGOT);
        reclaim(Items.IRON_LEGGINGS, Items.IRON_INGOT);
        reclaim(Items.IRON_BOOTS, Items.IRON_INGOT);
        reclaim(Items.IRON_SHOVEL, Items.IRON_INGOT);
        reclaim(Items.IRON_SWORD, Items.IRON_INGOT);
        reclaim(Items.IRON_PICKAXE, Items.IRON_INGOT);
        reclaim(Items.IRON_AXE, Items.IRON_INGOT);
        reclaim(Items.IRON_HOE, Items.IRON_INGOT);
        reclaim(Items.IRON_HORSE_ARMOR, Items.IRON_INGOT);

        reclaim(Items.GOLDEN_HELMET, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_CHESTPLATE, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_LEGGINGS, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_BOOTS, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_SHOVEL, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_SWORD, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_PICKAXE, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_AXE, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_HOE, Items.GOLD_INGOT);
        reclaim(Items.GOLDEN_HORSE_ARMOR, Items.GOLD_INGOT);

        reclaim(Items.DIAMOND_HELMET, Items.DIAMOND);
        reclaim(Items.DIAMOND_CHESTPLATE, Items.DIAMOND);
        reclaim(Items.DIAMOND_LEGGINGS, Items.DIAMOND);
        reclaim(Items.DIAMOND_BOOTS, Items.DIAMOND);
        reclaim(Items.DIAMOND_SHOVEL, Items.DIAMOND);
        reclaim(Items.DIAMOND_SWORD, Items.DIAMOND);
        reclaim(Items.DIAMOND_PICKAXE, Items.DIAMOND);
        reclaim(Items.DIAMOND_AXE, Items.DIAMOND);
        reclaim(Items.DIAMOND_HOE, Items.DIAMOND);
        reclaim(Items.DIAMOND_HORSE_ARMOR, Items.DIAMOND);
    }

    /**
     * 生成简单冲压配方
     *
     * @param item  原料
     * @param item1 产物
     */
    public static void stamping(RegistrateRecipeProvider provider, Item item, Item item1) {
        StampingRecipesLoader.provider = provider;
        stamping(item, item1);
    }

    private static void stamping(Item item, Item item1) {
        if (StampingRecipesLoader.provider == null) return;
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), item)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), item1)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(StampingRecipesLoader.provider,
                AnvilCraft.of("stamping/" + BuiltInRegistries.ITEM.getKey(item).getPath()
                    + "_2_" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }

    /**
     * 生成回收配方
     *
     * @param item  原料
     * @param item1 产物
     */
    public static void reclaim(RegistrateRecipeProvider provider, Item item, Item item1) {
        StampingRecipesLoader.provider = provider;
        reclaim(item, item1);
    }

    private static void reclaim(Item item, Item item1) {
        if (StampingRecipesLoader.provider == null) return;
        AnvilRecipe.Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), item)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item));
        if (item instanceof TieredItem) {
            builder.spawnItem(new Vec3(0.0, -0.75, 0.0), 0.5, item1);
        } else if (item instanceof ArmorItem || item instanceof HorseArmorItem) {
            builder.spawnItem(new Vec3(0.0, -0.75, 0.0), 0.5, item1);
            builder.spawnItem(new Vec3(0.0, -0.75, 0.0), 0.5, item1);
        } else {
            builder.spawnItem(new Vec3(0.0, -0.75, 0.0), item1);
        }
        builder.save(StampingRecipesLoader.provider,
            AnvilCraft.of("reclaim/" + BuiltInRegistries.ITEM.getKey(item).getPath()
                + "_2_" + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
