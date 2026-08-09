package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.util.RecipeLoaderUtil;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class SuperHeatingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        HolderGetter<Item> items = provider.getItems();
        SuperHeatingRecipe.builder()
            .transform(Blocks.LAVA_CAULDRON)
            .produce(1000)
            .requires(Items.COBBLESTONE, 4)
            .requires(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lava_from_cobblestone"));
        SuperHeatingRecipe.builder()
            .transform(Blocks.LAVA_CAULDRON)
            .produce(1000)
            .requires(items, Tags.Items.STONES, 4)
            .requires(ModItems.LIME_POWDER)
            .save(provider, AnvilCraft.of("super_heating/lava_from_stone"));

        // Royal Steel Ingot Recipes
        SuperHeatingRecipe.builder()
            .requires(Items.IRON_INGOT, 2)
            .requires(Items.DIAMOND)
            .requires(items, ModItemTags.GEMS)
            .result(ModItems.ROYAL_STEEL_INGOT, 1)
            .save(provider, AnvilCraft.of("super_heating/royal_steel_ingot_base"));

        SuperHeatingRecipe.builder()
            .requires(Items.IRON_INGOT, 2)
            .requires(Items.DIAMOND)
            .requires(items, ModItemTags.GEMS)
            .requires(Items.AMETHYST_SHARD)
            .result(ModItems.ROYAL_STEEL_INGOT, 2)
            .save(provider, AnvilCraft.of("super_heating/royal_steel_ingot_bonus_1"));

        SuperHeatingRecipe.builder()
            .requires(Items.IRON_INGOT, 2)
            .requires(Items.DIAMOND)
            .requires(items, ModItemTags.GEMS)
            .requires(Items.AMETHYST_SHARD, 2)
            .result(ModItems.ROYAL_STEEL_INGOT, 3)
            .save(provider, AnvilCraft.of("super_heating/royal_steel_ingot_bonus_2"));

        // Royal Steel Block Recipes
        SuperHeatingRecipe.builder()
            .requires(Blocks.IRON_BLOCK, 2)
            .requires(Blocks.DIAMOND_BLOCK)
            .requires(items, ModItemTags.GEM_BLOCKS)
            .result(ModBlocks.ROYAL_STEEL_BLOCK, 1)
            .save(provider, AnvilCraft.of("super_heating/royal_steel_block_base"));

        SuperHeatingRecipe.builder()
            .requires(Blocks.IRON_BLOCK, 2)
            .requires(Blocks.DIAMOND_BLOCK)
            .requires(items, ModItemTags.GEM_BLOCKS)
            .requires(Blocks.AMETHYST_BLOCK)
            .result(ModBlocks.ROYAL_STEEL_BLOCK, 2)
            .save(provider, AnvilCraft.of("super_heating/royal_steel_block_bonus_1"));

        SuperHeatingRecipe.builder()
            .requires(Blocks.IRON_BLOCK, 2)
            .requires(Blocks.DIAMOND_BLOCK)
            .requires(items, ModItemTags.GEM_BLOCKS)
            .requires(Blocks.AMETHYST_BLOCK, 2)
            .result(ModBlocks.ROYAL_STEEL_BLOCK, 3)
            .save(provider, AnvilCraft.of("super_heating/royal_steel_block_bonus_2"));

        SuperHeatingRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND, 8)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .result(ModBlocks.TEMPERING_GLASS, 8)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND, 8)
            .requires(ModItems.FROST_METAL_INGOT)
            .result(ModBlocks.FROST_GLASS, 8)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(ModBlocks.QUARTZ_SAND, 8)
            .requires(ModItems.EMBER_METAL_INGOT)
            .result(ModBlocks.EMBER_GLASS, 8)
            .save(provider);

        SuperHeatingRecipe.builder()
            .requires(Items.COPPER_INGOT, 2)
            .requires(items, ModItemTags.ZINC_INGOTS)
            .result(ModItems.BRASS_INGOT, 3)
            .save(provider);
        SuperHeatingRecipe.builder()
            .requires(Items.COPPER_INGOT, 2)
            .requires(items, ModItemTags.TIN_INGOTS)
            .result(ModItems.BRONZE_INGOT, 3)
            .save(provider);

        SuperHeatingRecipe.builder().requires(ModItems.WOOD_FIBER, 2).result(Items.CHARCOAL).save(provider);
        SuperHeatingRecipe.builder().requires(Blocks.COAL_BLOCK, 8).result(Items.DIAMOND).save(provider);

        SuperHeatingRecipe.builder().requires(ModBlocks.END_DUST).result(Items.END_STONE).save(provider);

        SuperHeatingRecipe.builder()
            .transform(ModBlocks.MELT_GEM_CAULDRON.get())
            .produce(1000)
            .requires(items, ModItemTags.GEM_BLOCKS)
            .save(provider, AnvilCraft.of("super_heating/melt_gem_cauldron_from_gem_block"));
        SuperHeatingRecipe.builder()
            .transform(ModBlocks.MELT_GEM_CAULDRON.get())
            .produce(1000)
            .requires(ModBlocks.CHROMATIC_STONE)
            .save(provider, AnvilCraft.of("super_heating/melt_gem_cauldron_from_chromatic_stone"));

        // metalBlockFromRaw
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, Tags.Items.STORAGE_BLOCKS_RAW_COPPER, Items.COPPER_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, Tags.Items.STORAGE_BLOCKS_RAW_IRON, Items.IRON_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, Tags.Items.STORAGE_BLOCKS_RAW_GOLD, Items.GOLD_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, ModItemTags.STORAGE_BLOCKS_RAW_ZINC, ModBlocks.ZINC_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, ModItemTags.STORAGE_BLOCKS_RAW_TIN, ModBlocks.TIN_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, ModItemTags.STORAGE_BLOCKS_RAW_TITANIUM, ModBlocks.TITANIUM_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, ModItemTags.STORAGE_BLOCKS_RAW_TUNGSTEN, ModBlocks.TUNGSTEN_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, ModItemTags.STORAGE_BLOCKS_RAW_LEAD, ModBlocks.LEAD_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, ModItemTags.STORAGE_BLOCKS_RAW_SILVER, ModBlocks.SILVER_BLOCK);
        SuperHeatingRecipeLoader.metalBlockFromRaw(provider, ModItemTags.STORAGE_BLOCKS_RAW_URANIUM, ModBlocks.URANIUM_BLOCK);

        // limePowder
        SuperHeatingRecipeLoader.limePowder(provider, ModItems.CRAB_CLAW, 1);
        SuperHeatingRecipeLoader.limePowder(provider, Items.NAUTILUS_SHELL, 1);
        SuperHeatingRecipeLoader.limePowder(provider, Items.POINTED_DRIPSTONE, 1);
        SuperHeatingRecipeLoader.limePowder(provider, Items.DRIPSTONE_BLOCK, 4);
        SuperHeatingRecipeLoader.limePowder(provider, Items.CALCITE, 4);
        SuperHeatingRecipeLoader.limePowder(provider, ModItemTags.DEAD_CORAL_BLOCKS, 4);
        SuperHeatingRecipeLoader.limePowder(provider, ModItemTags.DEAD_CORALS, 1);

        // ingotFromEarth
        SuperHeatingRecipeLoader.ingotFromEarth(provider, Tags.Items.RAW_MATERIALS_COPPER, Items.COPPER_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, Tags.Items.RAW_MATERIALS_IRON, Items.IRON_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, Tags.Items.RAW_MATERIALS_GOLD, Items.GOLD_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, ModItemTags.RAW_ZINC, ModItems.ZINC_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, ModItemTags.RAW_TIN, ModItems.TIN_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, ModItemTags.RAW_TITANIUM, ModItems.TITANIUM_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, ModItemTags.RAW_TUNGSTEN, ModItems.TUNGSTEN_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, ModItemTags.RAW_LEAD, ModItems.LEAD_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, ModItemTags.RAW_SILVER, ModItems.SILVER_INGOT);
        SuperHeatingRecipeLoader.ingotFromEarth(provider, ModItemTags.RAW_URANIUM, ModItems.URANIUM_INGOT);
    }

    private static void metalBlockFromRaw(RegistrumRecipeProvider provider, TagKey<Item> raw, ItemLike result) {
        HolderGetter<Item> items = provider.getItems();
        SuperHeatingRecipe.builder().requires(items, raw).result(result, 2).save(
            provider,
            AnvilCraft.of("super_heating/metal_block/%s_from_%s".formatted(RecipeLoaderUtil.getName(result), RecipeLoaderUtil.getName(raw)))
        );
    }

    private static void ingotFromEarth(RegistrumRecipeProvider provider, TagKey<Item> raw, ItemLike result) {
        HolderGetter<Item> items = provider.getItems();
        SuperHeatingRecipe.builder()
            .requires(items, raw, 8)
            .requires(ModItems.EARTH_CORE_SHARD)
            .result(result, 24)
            .save(provider, AnvilCraft.of("super_heating/raw/%s".formatted(RecipeLoaderUtil.getName(result))));
    }

    private static void limePowder(RegistrumRecipeProvider provider, ItemLike item, int resultCount) {
        SuperHeatingRecipe.builder()
            .requires(item)
            .result(ModItems.LIME_POWDER, resultCount)
            .save(provider, AnvilCraft.of("super_heating/lime_powder/%s_from_%s".formatted("lime_powder", RecipeLoaderUtil.getName(item))));
    }

    private static void limePowder(RegistrumRecipeProvider provider, TagKey<Item> tag, int resultCount) {
        HolderGetter<Item> items = provider.getItems();
        SuperHeatingRecipe.builder()
            .requires(items, tag)
            .result(ModItems.LIME_POWDER, resultCount)
            .save(provider, AnvilCraft.of("super_heating/lime_powder/%s_from_%s".formatted("lime_powder", RecipeLoaderUtil.getName(tag))));
    }
}
