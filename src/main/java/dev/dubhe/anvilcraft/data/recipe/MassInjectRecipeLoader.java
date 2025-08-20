package dev.dubhe.anvilcraft.data.recipe;

import com.google.common.collect.ImmutableMap;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

public class MassInjectRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ImmutableMap<TagKey<Item>, Integer> tagRecipes = ImmutableMap.<TagKey<Item>, Integer>builder()
            .put(ModItemTags.TITANIUM_NUGGETS, 5)
            .put(ModItemTags.ZINC_NUGGETS, 7)
            .put(ModItemTags.TIN_NUGGETS, 7)
            .put(Tags.Items.NUGGETS_IRON, 8)
            .put(ModItemTags.BRONZE_NUGGETS, 8)
            .put(ModItemTags.BRASS_NUGGETS, 8)
            .put(ModItemTags.COPPER_NUGGETS, 9)
            .put(ModItemTags.SILVER_NUGGETS, 10)
            .put(ModItemTags.LEAD_NUGGETS, 11)
            .put(ModItemTags.URANIUM_NUGGETS, 18)
            .put(ModItemTags.TUNGSTEN_NUGGETS, 19)
            .put(Tags.Items.NUGGETS_GOLD, 19)
            .put(ModItemTags.TITANIUM_INGOTS, 50)
            .put(ModItemTags.ZINC_INGOTS, 70)
            .put(ModItemTags.TIN_INGOTS, 70)
            .put(Tags.Items.INGOTS_IRON, 80)
            .put(ModItemTags.BRONZE_INGOTS, 80)
            .put(ModItemTags.BRASS_INGOTS, 80)
            .put(Tags.Items.INGOTS_COPPER, 90)
            .put(ModItemTags.SILVER_INGOTS, 100)
            .put(ModItemTags.LEAD_INGOTS, 110)
            .put(ModItemTags.NETHERITE_NUGGETS, 150)
            .put(ModItemTags.URANIUM_INGOTS, 180)
            .put(ModItemTags.TUNGSTEN_INGOTS, 190)
            .put(Tags.Items.INGOTS_GOLD, 190)
            .put(ModItemTags.STORAGE_BLOCKS_TITANIUM, 500)
            .put(ModItemTags.STORAGE_BLOCKS_ZINC, 700)
            .put(ModItemTags.STORAGE_BLOCKS_TIN, 700)
            .put(Tags.Items.STORAGE_BLOCKS_IRON, 800)
            .put(ModItemTags.STORAGE_BLOCKS_BRONZE, 800)
            .put(ModItemTags.STORAGE_BLOCKS_BRASS, 800)
            .put(Tags.Items.STORAGE_BLOCKS_COPPER, 900)
            .put(ModItemTags.STORAGE_BLOCKS_SILVER, 1000)
            .put(ModItemTags.STORAGE_BLOCKS_LEAD, 1100)
            .put(Tags.Items.INGOTS_NETHERITE, 1500)
            .put(ModItemTags.STORAGE_BLOCKS_URANIUM, 1800)
            .put(ModItemTags.STORAGE_BLOCKS_TUNGSTEN, 1900)
            .put(Tags.Items.STORAGE_BLOCKS_GOLD, 1900)
            .put(Tags.Items.STORAGE_BLOCKS_NETHERITE, 15000)
            .put(ModItemTags.PLUTONIUM_NUGGETS, 19)
            .put(ModItemTags.PLUTONIUM_INGOTS, 190)
            .put(ModItemTags.STORAGE_BLOCKS_PLUTONIUM, 1900)
            .put(ModItemTags.TRANSCENDIUM_NUGGETS, 250)
            .put(ModItemTags.TRANSCENDIUM_INGOTS, 2500)
            .put(ModItemTags.STORAGE_BLOCKS_TRANSCENDIUM, 25000)
            .put(ModItemTags.FROST_METAL_NUGGETS, 30)
            .put(ModItemTags.FROST_METAL_INGOTS, 300)
            .put(ModItemTags.STORAGE_BLOCKS_FROST_METAL, 3000)
            .build();
        tagRecipes.forEach((tag, mass) -> addTag(provider, tag, mass));

        ImmutableMap<ItemLike, Integer> itemRecipes = ImmutableMap.<ItemLike, Integer>builder()
            .put(ModItems.CURSED_GOLD_NUGGET, 25)
            .put(ModItems.ROYAL_STEEL_NUGGET, 40)
            .put(ModItems.EMBER_METAL_NUGGET, 200)
            .put(ModItems.CURSED_GOLD_INGOT, 250)
            .put(ModItems.ROYAL_STEEL_INGOT, 400)
            .put(ModItems.EMBER_METAL_INGOT, 2000)
            .put(ModBlocks.CURSED_GOLD_BLOCK, 2500)
            .put(ModBlocks.ROYAL_STEEL_BLOCK, 4000)
            .put(ModBlocks.HEAVY_IRON_BLOCK, 8000)
            .put(ModBlocks.EMBER_METAL_BLOCK, 20000)
            .build();
        itemRecipes.forEach((item, mass) -> addItem(provider, item, mass));
    }

    private static void addTag(RegistrateRecipeProvider provider, TagKey<Item> tag, int mass) {
        MassInjectRecipe.builder().requires(tag).mass(mass).save(provider);
    }

    private static void addItem(RegistrateRecipeProvider provider, ItemLike item, int mass) {
        MassInjectRecipe.builder().requires(item).mass(mass).save(provider);
    }
}
