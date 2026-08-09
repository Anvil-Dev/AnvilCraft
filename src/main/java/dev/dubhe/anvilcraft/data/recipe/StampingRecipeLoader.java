package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingDiffRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class StampingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        final HolderGetter<Item> items = provider.getItems();
        StampingRecipeLoader.stamping(provider, Items.IRON_INGOT, Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, Items.GOLD_INGOT, Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, Items.COPPER_INGOT, ModBlocks.COPPER_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.TUNGSTEN_INGOTS, ModBlocks.TUNGSTEN_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.TITANIUM_INGOTS, ModBlocks.TITANIUM_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.ZINC_INGOTS, ModBlocks.ZINC_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.TIN_INGOTS, ModBlocks.TIN_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.LEAD_INGOTS, ModBlocks.LEAD_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.SILVER_INGOTS, ModBlocks.SILVER_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.URANIUM_INGOTS, ModBlocks.URANIUM_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.PLUTONIUM_INGOTS, ModBlocks.PLUTONIUM_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.BRONZE_INGOTS, ModBlocks.BRONZE_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, ModItemTags.BRASS_INGOTS, ModBlocks.BRASS_PRESSURE_PLATE);
        StampingRecipeLoader.stamping(provider, Items.SNOWBALL, Items.SNOW);
        StampingRecipeLoader.stamping(provider, Items.CHERRY_LEAVES, Items.PINK_PETALS);
        StampingRecipe.builder()
            .requires(ModItems.WOOD_FIBER)
            .result(Items.PAPER, 4)
            .save(provider, AnvilCraft.of("stamping/paper_from_wood_fiber"));

        StampingRecipe.builder()
            .requires(Items.MILK_BUCKET)
            .result(ModItems.CREAM, 4)
            .result(Items.BUCKET)
            .save(provider, AnvilCraft.of("stamping/cream"));
        StampingRecipe.builder()
            .requires(Items.SUGAR_CANE)
            .result(Items.PAPER)
            .result(Items.SUGAR)
            .save(provider, AnvilCraft.of("stamping/paper_from_sugar_cane"));
        StampingRecipe.builder()
            .requires(Items.HEART_OF_THE_SEA)
            .result(ModItems.SEA_HEART_SHELL_SHARD, 3)
            .result(ModItems.SEA_HEART_SHELL_SHARD, 0.5F)
            .result(ModItems.SEA_HEART_SHELL_SHARD, 0.5F)
            .result(ModItems.SAPPHIRE)
            .save(provider);
        StampingRecipe.builder()
            .requires(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
            .requires(ModItems.EMBER_METAL_INGOT)
            .result(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE)
            .save(provider);
        StampingRecipe.builder()
            .requires(items, ModItemTags.TIN_PLATES)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .result(ModItems.TIN_CAN)
            .result(ModItems.ROYAL_STEEL_INGOT)
            .save(provider, AnvilCraft.of("stamping/tin_can_from_plate"));
        StampingRecipe.builder()
            .requires(ModItems.GEODE)
            .result(Items.AMETHYST_SHARD, 4)
            .result(ModItems.TOPAZ.get(), 0.25F)
            .result(ModItems.SAPPHIRE.get(), 0.25F)
            .result(ModItems.RUBY.get(), 0.25F)
            .save(provider, AnvilCraft.of("stamping/geode_gems"));
        StampingRecipe.builder()
            .requires(Items.COCOA_BEANS)
            .result(ModItems.COCOA_BUTTER)
            .result(ModItems.COCOA_POWDER)
            .save(provider);
        StampingRecipe.builder()
            .requires(ModItems.PRISMARINE_CLUSTER)
            .result(Items.PRISMARINE_CRYSTALS, 2)
            .result(Items.PRISMARINE_SHARD)
            .result(Items.PRISMARINE_CRYSTALS, 0.5F)
            .result(ModItems.PRISMARINE_BLADE, 0.15F)
            .save(provider);

        StampingDiffRecipe.builder()
            .requires(items, ModItemTags.TEMPLATES, 2)
            .result(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE)
            .save(provider);
        StampingDiffRecipe.builder()
            .requires(items, ModItemTags.TEMPLATES, 4)
            .result(ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE)
            .save(provider);
        StampingDiffRecipe.builder()
            .requires(items, ModItemTags.TEMPLATES, 8)
            .result(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE)
            .save(provider);
    }

    @SuppressWarnings("SameParameterValue")
    private static void stamping(RegistrumRecipeProvider provider, ItemLike input, ItemLike result, int count) {
        StampingRecipe.builder()
            .requires(input)
            .result(result, count)
            .save(provider);
    }

    private static void stamping(RegistrumRecipeProvider provider, ItemLike input, ItemLike result) {
        StampingRecipeLoader.stamping(provider, input, result, 1);
    }

    private static void stamping(RegistrumRecipeProvider provider, TagKey<Item> input, ItemLike result) {
        HolderGetter<Item> items = provider.getItems();
        StampingRecipe.builder()
            .requires(items, input)
            .result(result, 1)
            .save(provider);
    }
}
