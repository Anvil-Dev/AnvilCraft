package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.ItemCrushRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class ItemCrushRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        itemCrush(provider, Items.WET_SPONGE, new ItemStack(ModItems.SPONGE_GEMMULE.asItem(), 4));
        itemCrush(provider, Items.MELON, new ItemStack(Items.MELON_SLICE, 9));
        itemCrush(provider, Items.SNOW_BLOCK, new ItemStack(Items.SNOWBALL, 4));
        itemCrush(provider, Items.CLAY, new ItemStack(Items.CLAY_BALL, 4));
        itemCrush(provider, Items.GLOWSTONE, new ItemStack(Items.GLOWSTONE_DUST, 4));
        itemCrush(provider, Items.QUARTZ_BLOCK, new ItemStack(Items.QUARTZ, 4));
        itemCrush(provider, Items.DRIPSTONE_BLOCK, new ItemStack(Items.POINTED_DRIPSTONE, 4));
        itemCrush(provider, Items.AMETHYST_BLOCK, new ItemStack(Items.AMETHYST_SHARD, 4));
        itemCrush(provider, Items.HONEYCOMB_BLOCK, new ItemStack(Items.HONEYCOMB, 4));

        ItemCrushRecipe.builder()
                .requires(Items.HONEY_BLOCK)
                .requires(Items.GLASS_BOTTLE, 4)
                .result(new ItemStack(Items.HONEY_BOTTLE, 4))
                .save(provider);

        ItemCrushRecipe.builder()
                .requires(ModBlocks.HOLLOW_MAGNET_BLOCK)
                .result(new ItemStack(ModItems.MAGNET_INGOT.asItem(), 8))
                .save(provider, AnvilCraft.of("item_crush/magnet_ingot_from_hollow_magnet_block"));
        ItemCrushRecipe.builder()
                .requires(ModBlocks.MAGNET_BLOCK)
                .result(new ItemStack(ModItems.MAGNET_INGOT.asItem(), 9))
                .save(provider, AnvilCraft.of("item_crush/magnet_ingot_from_magnet_block"));

        ItemCrushRecipe.builder()
                .requires(Items.PRISMARINE)
                .result(new ItemStack(Items.PRISMARINE_SHARD, 4))
                .save(provider, AnvilCraft.of("item_crush/prismine_shard_from_prismine"));

        ItemCrushRecipe.builder()
                .requires(Items.PRISMARINE_BRICKS)
                .result(new ItemStack(Items.PRISMARINE_SHARD, 9))
                .save(provider, AnvilCraft.of("item_crush/prismine_shard_from_prismine_bricks"));
    }

    private static void itemCrush(RegistrateRecipeProvider provider, ItemLike input, ItemStack result) {
        ItemCrushRecipe.builder().requires(input).result(result).save(provider);
    }
}
