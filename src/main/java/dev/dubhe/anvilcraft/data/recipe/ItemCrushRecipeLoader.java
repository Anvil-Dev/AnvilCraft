package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.recipe.outcome.SpawnItem;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.util.RecipeLoaderUtil;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.Tags;

public class ItemCrushRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        HolderGetter<Item> items = provider.getItems();
        ItemCrushRecipe.builder()
            .requires(items, Tags.Items.CROPS_WHEAT)
            .result(ModItems.FLOUR)
            .result(ModItems.FLOUR, 0.5F)
            .save(provider);
        ItemCrushRecipe.builder().requires(items, ItemTags.LOGS).result(ModItems.WOOD_FIBER).result(ModItems.RESIN).save(provider);
        ItemCrushRecipe.builder()
            .requires(Items.BEETROOT)
            .result(Items.RED_DYE)
            .result(Items.SUGAR)
            .save(provider, AnvilCraft.of("item_crush/red_dye_from_beetroot"));
        ItemCrushRecipe.builder().requires(items, ItemTags.WOOL).result(Items.STRING, 4).save(provider);
        ItemCrushRecipe.builder()
            .requires(Items.BONE)
            .result(Items.BONE_MEAL, 6)
            .save(provider, AnvilCraft.of("item_crush/bone_meal_from_bone"));
        ItemCrushRecipe.builder().requires(Items.BLAZE_ROD).result(Items.BLAZE_POWDER, 4).save(provider);
        ItemCrushRecipe.builder().requires(Items.BREEZE_ROD).result(Items.WIND_CHARGE, 8).save(provider);
        ItemCrushRecipe.builder().requires(Items.LEATHER).result(Items.RABBIT_HIDE, 4).save(provider);
        ItemCrushRecipe.builder()
            .requires(ModItems.GEODE)
            .result(Items.AMETHYST_SHARD, 4)
            .result(ModItems.TOPAZ.get(), 0.25F)
            .result(ModItems.SAPPHIRE.get(), 0.25F)
            .result(ModItems.RUBY.get(), 0.25F)
            .save(provider, AnvilCraft.of("item_crush/geode_gems"));

        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .group("item_crush")
            .icon(new ItemStackTemplate(ModBlocks.CRUSHING_TABLE.asItem()))
            .hasBlock(0, -1, 0, ModBlocks.CRUSHING_TABLE.getDefaultState())
            .hasItemIngredient(builder -> builder.offset(0, -.125, 0).range(.75, .75, .75).of(ModBlocks.CHROMATIC_STONE))
            .chooseOne(builder -> builder.choice(SpawnItem.builder().offset(0, -.6875, 0).item(ModItems.RUBY).build(), .25F)
                .choice(SpawnItem.builder().offset(0, -.6875, 0).item(ModItems.TOPAZ).build(), .25F)
                .choice(SpawnItem.builder().offset(0, -.6875, 0).item(ModItems.SAPPHIRE).build(), .25F)
                .choice(SpawnItem.builder().offset(0, -.6875, 0).item(Items.EMERALD).build(), .25F))
            .save(provider, AnvilCraft.of("item_crush/gem_from_chromatic_stone"));

        ItemCrushRecipe.builder().requires(Items.CREEPER_HEAD).result(Items.GUNPOWDER, 64).save(provider);

        ItemCrushRecipe.builder()
            .requires(Items.SKELETON_SKULL)
            .result(Items.BONE_MEAL, 64)
            .save(provider, AnvilCraft.of("item_crush/bone_meal_from_skeleton_skull"));

        ItemCrushRecipeLoader.armor(provider, Items.CHAINMAIL_HELMET, Items.IRON_CHAIN);
        ItemCrushRecipeLoader.armor(provider, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHAIN);
        ItemCrushRecipeLoader.armor(provider, Items.CHAINMAIL_LEGGINGS, Items.IRON_CHAIN);
        ItemCrushRecipeLoader.armor(provider, Items.CHAINMAIL_BOOTS, Items.IRON_CHAIN);

        ItemCrushRecipeLoader.armor(provider, Items.LEATHER_HELMET, Items.LEATHER);
        ItemCrushRecipeLoader.armor(provider, Items.LEATHER_CHESTPLATE, Items.LEATHER);
        ItemCrushRecipeLoader.armor(provider, Items.LEATHER_LEGGINGS, Items.LEATHER);
        ItemCrushRecipeLoader.armor(provider, Items.LEATHER_BOOTS, Items.LEATHER);
        ItemCrushRecipeLoader.armor(provider, Items.LEATHER_HORSE_ARMOR, Items.LEATHER);

        ItemCrushRecipeLoader.tool(provider, Items.IRON_SWORD, Items.IRON_INGOT);
        ItemCrushRecipeLoader.tool(provider, Items.IRON_PICKAXE, Items.IRON_INGOT);
        ItemCrushRecipeLoader.tool(provider, Items.IRON_AXE, Items.IRON_INGOT);
        ItemCrushRecipeLoader.tool(provider, Items.IRON_HOE, Items.IRON_INGOT);
        ItemCrushRecipeLoader.tool(provider, Items.IRON_SHOVEL, Items.IRON_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.IRON_HELMET, Items.IRON_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.IRON_CHESTPLATE, Items.IRON_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.IRON_LEGGINGS, Items.IRON_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.IRON_BOOTS, Items.IRON_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.IRON_HORSE_ARMOR, Items.IRON_INGOT);

        ItemCrushRecipeLoader.tool(provider, Items.GOLDEN_SWORD, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.tool(provider, Items.GOLDEN_PICKAXE, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.tool(provider, Items.GOLDEN_AXE, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.tool(provider, Items.GOLDEN_HOE, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.tool(provider, Items.GOLDEN_SHOVEL, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.GOLDEN_HELMET, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.GOLDEN_CHESTPLATE, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.GOLDEN_LEGGINGS, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.GOLDEN_BOOTS, Items.GOLD_INGOT);
        ItemCrushRecipeLoader.armor(provider, Items.GOLDEN_HORSE_ARMOR, Items.GOLD_INGOT);

        ItemCrushRecipeLoader.tool(provider, Items.DIAMOND_SWORD, Items.DIAMOND);
        ItemCrushRecipeLoader.tool(provider, Items.DIAMOND_PICKAXE, Items.DIAMOND);
        ItemCrushRecipeLoader.tool(provider, Items.DIAMOND_AXE, Items.DIAMOND);
        ItemCrushRecipeLoader.tool(provider, Items.DIAMOND_HOE, Items.DIAMOND);
        ItemCrushRecipeLoader.tool(provider, Items.DIAMOND_SHOVEL, Items.DIAMOND);
        ItemCrushRecipeLoader.armor(provider, Items.DIAMOND_HELMET, Items.DIAMOND);
        ItemCrushRecipeLoader.armor(provider, Items.DIAMOND_CHESTPLATE, Items.DIAMOND);
        ItemCrushRecipeLoader.armor(provider, Items.DIAMOND_LEGGINGS, Items.DIAMOND);
        ItemCrushRecipeLoader.armor(provider, Items.DIAMOND_BOOTS, Items.DIAMOND);
        ItemCrushRecipeLoader.armor(provider, Items.DIAMOND_HORSE_ARMOR, Items.DIAMOND);

        ItemCrushRecipeLoader.blockCrush(provider, Items.STONE, Items.COBBLESTONE);
        ItemCrushRecipeLoader.blockCrush(provider, Items.COBBLESTONE, Items.GRAVEL);
        ItemCrushRecipeLoader.blockCrush(provider, Items.GRAVEL, Items.SAND);
        ItemCrushRecipeLoader.blockCrush(provider, Items.POLISHED_GRANITE, Items.GRANITE);
        ItemCrushRecipeLoader.blockCrush(provider, Items.GRANITE, Items.RED_SAND);
        ItemCrushRecipeLoader.blockCrush(provider, Items.POLISHED_ANDESITE, Items.ANDESITE);
        ItemCrushRecipeLoader.blockCrush(provider, Items.ANDESITE, ModBlocks.CINERITE.get());
        ItemCrushRecipeLoader.blockCrush(provider, Items.POLISHED_DIORITE, Items.DIORITE);
        ItemCrushRecipeLoader.blockCrush(provider, Items.DIORITE, ModBlocks.QUARTZ_SAND.get());
        ItemCrushRecipeLoader.blockCrush(provider, Items.STONE_BRICKS, Items.CRACKED_STONE_BRICKS);
        ItemCrushRecipeLoader.blockCrush(provider, Items.DEEPSLATE_BRICKS, Items.CRACKED_DEEPSLATE_BRICKS);
        ItemCrushRecipeLoader.blockCrush(provider, Items.NETHER_BRICKS, Items.CRACKED_NETHER_BRICKS);
        ItemCrushRecipeLoader.blockCrush(provider, Items.DEEPSLATE_TILES, Items.CRACKED_DEEPSLATE_TILES);
        ItemCrushRecipeLoader.blockCrush(provider, Items.POLISHED_BLACKSTONE_BRICKS, Items.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        ItemCrushRecipeLoader.blockCrush(provider, Items.SOUL_SOIL, Items.SOUL_SAND);
        ItemCrushRecipeLoader.blockCrush(provider, Items.NETHERRACK, ModBlocks.NETHER_DUST.get());
        ItemCrushRecipeLoader.blockCrush(provider, Items.END_STONE, ModBlocks.END_DUST.get());

        ItemCrushRecipeLoader.flower(provider, Items.LILY_OF_THE_VALLEY, Items.WHITE_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.AZURE_BLUET, Items.LIGHT_GRAY_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.OXEYE_DAISY, Items.LIGHT_GRAY_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.WHITE_TULIP, Items.LIGHT_GRAY_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.WITHER_ROSE, Items.BLACK_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.POPPY, Items.RED_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.ROSE_BUSH, Items.RED_DYE, 4);
        ItemCrushRecipeLoader.flower(provider, Items.RED_TULIP, Items.RED_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.ORANGE_TULIP, Items.ORANGE_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.TORCHFLOWER, Items.ORANGE_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.DANDELION, Items.YELLOW_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.SUNFLOWER, Items.YELLOW_DYE, 4);
        ItemCrushRecipeLoader.flower(provider, Items.PITCHER_PLANT, Items.CYAN_DYE, 4);
        ItemCrushRecipeLoader.flower(provider, Items.BLUE_ORCHID, Items.LIGHT_BLUE_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.CORNFLOWER, Items.BLUE_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.ALLIUM, Items.MAGENTA_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.LILAC, Items.MAGENTA_DYE, 4);
        ItemCrushRecipeLoader.flower(provider, Items.PEONY, Items.PINK_DYE, 4);
        ItemCrushRecipeLoader.flower(provider, Items.PINK_PETALS, Items.PINK_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.PINK_TULIP, Items.PINK_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.BONE_MEAL, Items.WHITE_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.INK_SAC, Items.BLACK_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.COCOA_BEANS, Items.BROWN_DYE);
        ItemCrushRecipeLoader.flower(provider, Items.LAPIS_LAZULI, Items.BLUE_DYE);
    }

    private static void tool(RegistrumRecipeProvider provider, ItemLike tool, ItemLike result) {
        ItemCrushRecipe.builder()
            .requires(tool)
            .result(result, 0.5F)
            .save(
                provider,
                AnvilCraft.of("item_crush/tool/%s_2_%s".formatted(RecipeLoaderUtil.getName(tool), RecipeLoaderUtil.getName(result))));
    }

    private static void blockCrush(RegistrumRecipeProvider provider, ItemLike input, ItemLike result) {
        ItemCrushRecipe.builder().requires(input).result(result, 0.8F).save(
            provider,
            AnvilCraft.of("item_crush/block_crush/%s_from_%s".formatted(RecipeLoaderUtil.getName(result), RecipeLoaderUtil.getName(input)))
        );
    }

    private static void armor(RegistrumRecipeProvider provider, ItemLike armor, ItemLike result) {
        ItemCrushRecipe.builder().requires(armor).result(result, UniformGenerator.between(0.0F, 2.0F)).save(
            provider,
            AnvilCraft.of("item_crush/armor/%s_2_%s".formatted(RecipeLoaderUtil.getName(armor), RecipeLoaderUtil.getName(result)))
        );
    }

    public static void flower(RegistrumRecipeProvider provider, ItemLike flower, ItemLike result) {
        ItemCrushRecipe.builder().requires(flower).result(result, 2).save(
            provider,
            AnvilCraft.of("item_crush/flower/%s_from_%s".formatted(RecipeLoaderUtil.getName(result), RecipeLoaderUtil.getName(flower)))
        );
    }

    public static void flower(RegistrumRecipeProvider provider, ItemLike flower, ItemLike result, int resultNum) {
        ItemCrushRecipe.builder().requires(flower).result(result, resultNum).save(
            provider,
            AnvilCraft.of("item_crush/flower/%s_from_%s".formatted(RecipeLoaderUtil.getName(result), RecipeLoaderUtil.getName(flower)))
        );
    }

}
