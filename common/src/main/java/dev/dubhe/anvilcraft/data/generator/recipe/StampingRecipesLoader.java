package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe.Builder;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class StampingRecipesLoader {
    private static RegistrateRecipeProvider provider = null;

    /**
     * 初始化配方
     *
     * @param provider 配方提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        StampingRecipesLoader.provider = provider;
        stamping(Items.IRON_INGOT, RecipeItem.of(Items.HEAVY_WEIGHTED_PRESSURE_PLATE));
        stamping(Items.GOLD_INGOT, RecipeItem.of(Items.LIGHT_WEIGHTED_PRESSURE_PLATE));
        stamping(Items.SNOWBALL, RecipeItem.of(Items.SNOW));
        stamping(ModItems.WOOD_FIBER.get(), RecipeItem.of(Items.PAPER));
        stamping(Items.MILK_BUCKET, RecipeItem.of(ModItems.CREAM.get(), 4));
        stamping(Items.SUGAR_CANE, RecipeItem.of(Items.PAPER), RecipeItem.of(Items.SUGAR, 0.25d));
        stamping(
            Items.COCOA_BEANS,
            RecipeItem.of(ModItems.COCOA_BUTTER),
            RecipeItem.of(ModItems.COCOA_POWDER)
        );
        stamping(
            Items.HEART_OF_THE_SEA,
            RecipeItem.of(ModItems.SEA_HEART_SHELL_SHARD, 3),
            RecipeItem.of(ModItems.SEA_HEART_SHELL_SHARD, 0.5d),
            RecipeItem.of(ModItems.SEA_HEART_SHELL_SHARD, 0.5d),
            RecipeItem.of(ModItems.SAPPHIRE));
        stamping(
            ModItems.PRISMARINE_CLUSTER.get(),
            RecipeItem.of(Items.PRISMARINE_CRYSTALS, 2),
            RecipeItem.of(ModItems.PRISMARINE_BLADE, 0.15d),
            RecipeItem.of(Items.PRISMARINE_CRYSTALS, 0.5d),
            RecipeItem.of(Items.PRISMARINE_SHARD));
        stamping(ModItems.GEODE.get(),
            RecipeItem.of(Items.AMETHYST_SHARD, 8),
            RecipeItem.of(ModItems.TOPAZ, 1, true),
            RecipeItem.of(ModItems.SAPPHIRE, 1, true),
            RecipeItem.of(ModItems.RUBY, 1, true)
        );
        stamping(
            ItemTags.LOGS,
            RecipeItem.of(ModItems.WOOD_FIBER, 1),
            RecipeItem.of(ModItems.RESIN, 0.25d));
        stamping(
            Items.CHERRY_LEAVES,
            RecipeItem.of(Items.PINK_PETALS));

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
        stamping(item, RecipeItem.of(item1));
    }

    private static void stamping(Item enter, RecipeItem... items) {
        if (StampingRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), enter)
            .type(AnvilRecipeType.STAMPING);
        SelectOne selectOne = new SelectOne();
        for (RecipeItem item : items) {
            if (item.isSelectOne()) {
                selectOne.add(new SpawnItem(
                    new Vec3(0.0, -0.75, 0.0),
                    item.getChance(),
                    new ItemStack(item.getItem()))
                );
            } else builder = builder.spawnItem(new Vec3(0.0, -0.75, 0.0), item);
        }
        if (!selectOne.isEmpty()) builder = builder.addOutcomes(selectOne);
        builder
            .unlockedBy(AnvilCraftDatagen.hasItem(enter), AnvilCraftDatagen.has(enter))
            .save(StampingRecipesLoader.provider,
                AnvilCraft.of("stamping/" + BuiltInRegistries.ITEM.getKey(enter).getPath()
                    + "_2_" + BuiltInRegistries.ITEM.getKey(Arrays.stream(items).toList()
                    .get(0).getItem().asItem()).getPath()));
    }

    private static void stamping(TagKey<Item> enter, RecipeItem... items) {
        if (StampingRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), enter)
            .type(AnvilRecipeType.STAMPING);
        SelectOne selectOne = new SelectOne();
        for (RecipeItem item : items) {
            if (item.isSelectOne()) {
                selectOne.add(new SpawnItem(
                    new Vec3(0.0, -0.75, 0.0),
                    item.getChance(),
                    new ItemStack(item.getItem()))
                );
            } else builder = builder.spawnItem(new Vec3(0.0, -0.75, 0.0), item);
        }
        if (!selectOne.isEmpty()) builder = builder.addOutcomes(selectOne);
        builder
            .unlockedBy(AnvilCraftDatagen.hasItem(enter), AnvilCraftDatagen.has(enter))
            .save(StampingRecipesLoader.provider,
                AnvilCraft.of("stamping/" + enter.location().getPath()
                    + "_2_" + BuiltInRegistries.ITEM.getKey(Arrays.stream(items).toList()
                    .get(0).getItem().asItem()).getPath()));
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
