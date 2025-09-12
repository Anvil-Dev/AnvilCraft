package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.anvilcraft.lib.data.advancement.predicate.item.NotPredicate;
import dev.anvilcraft.lib.init.LibItemSubPredicates;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.anvilcraft.lib.recipe.outcome.SpawnItem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemSubPredicates;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.item.property.predicate.ItemSavedEntityPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class TimeWarpRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        timeWarp(provider, ModItems.RESIN, 1, ModItems.AMBER, 1);
        timeWarp(provider, Items.OBSIDIAN, 1, Items.CRYING_OBSIDIAN, 1);
        timeWarp(provider, Items.CHARCOAL, 1, Items.COAL, 2);
        timeWarp(provider, Items.SAND, 1, Items.DIRT, 1);
        timeWarp(provider, Items.IRON_BLOCK, 1, Items.RAW_IRON, 3);
        timeWarp(provider, Items.GOLD_BLOCK, 1, Items.RAW_GOLD, 3);
        timeWarp(provider, Items.COPPER_BLOCK, 1, Items.RAW_COPPER, 3);
        timeWarp(provider, ModItems.GEODE, 1, Items.BUDDING_AMETHYST, 1);
        timeWarp(provider, ModBlocks.CINERITE, 1, Items.TUFF, 1);
        timeWarp(provider, ModBlocks.NETHER_DUST, 1, Items.SOUL_SOIL, 1);
        timeWarp(provider, ModBlocks.END_DUST, 1, Items.END_STONE, 1);
        timeWarp(provider, ModItems.LIME_POWDER, 8, Items.CALCITE, 1);
        timeWarp(provider, ModItems.NETHERITE_CRYSTAL_NUCLEUS, 1, Items.ANCIENT_DEBRIS, 1);
        timeWarp(provider, ModItemTags.STORAGE_BLOCKS_ZINC, 1, ModItems.RAW_ZINC, 3);
        timeWarp(provider, ModItemTags.STORAGE_BLOCKS_TIN, 1, ModItems.RAW_TIN, 3);
        timeWarp(provider, ModItemTags.STORAGE_BLOCKS_TITANIUM, 1, ModItems.RAW_TITANIUM, 3);
        timeWarp(provider, ModItemTags.STORAGE_BLOCKS_TUNGSTEN, 1, ModItems.RAW_TUNGSTEN, 3);
        timeWarp(provider, ModItemTags.STORAGE_BLOCKS_LEAD, 1, ModItems.RAW_LEAD, 3);
        timeWarp(provider, ModItemTags.STORAGE_BLOCKS_SILVER, 1, ModItems.RAW_SILVER, 3);
        timeWarp(provider, ModItems.SEA_HEART_SHELL_SHARD, 1, ModItems.SEA_HEART_SHELL, 1);

        TimeWarpRecipe.builder()
            .requires(Items.EMERALD)
            .result(Items.EMERALD_BLOCK)
            .consume(1000)
            .fluid(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItemTags.GEMS_RUBY)
            .result(ModBlocks.RUBY_BLOCK)
            .consume(1000)
            .fluid(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItemTags.GEMS_TOPAZ)
            .result(ModBlocks.TOPAZ_BLOCK)
            .consume(1000)
            .fluid(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItemTags.GEMS_SAPPHIRE)
            .result(ModBlocks.SAPPHIRE_BLOCK)
            .consume(1000)
            .fluid(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ItemTags.LOGS)
            .result(Items.COAL)
            .save(provider, AnvilCraft.of("time_warp/coal_from_logs"));

        timeWarpToOilCauldron(provider, Items.ROTTEN_FLESH, 64);
        timeWarpToOilCauldron(provider, Items.SPIDER_EYE, 64);
        timeWarpToOilCauldron(provider, ModItemTags.RAW_CHICKEN, 64);
        timeWarpToOilCauldron(provider, Tags.Items.FOODS_RAW_FISH, 64);
        timeWarpToOilCauldron(provider, ModItemTags.RAW_BEEF, 16);
        timeWarpToOilCauldron(provider, ModItemTags.RAW_PORKCHOP, 16);
        timeWarpToOilCauldron(provider, ModItemTags.RAW_MUTTON, 16);
        timeWarpToOilCauldron(provider, ModItemTags.RAW_RABBIT, 16);

        timeWarpToOilCauldron(provider, Items.ZOMBIE_HEAD, 1);
        timeWarpToOilCauldron(provider, Items.PIGLIN_HEAD, 1);

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .transform(ModBlocks.FIRE_CAULDRON.get())
            .result(ModItems.EMBER_METAL_INGOT, 3)
            .fluid(ModBlocks.FIRE_CAULDRON.get())
            .consume(1000)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_ingot_0"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 1)
            .transform(ModBlocks.FIRE_CAULDRON.get())
            .result(ModItems.EMBER_METAL_INGOT, 4)
            .fluid(ModBlocks.FIRE_CAULDRON.get())
            .consume(1000)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_ingot_1"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 2)
            .transform(ModBlocks.FIRE_CAULDRON.get())
            .result(ModItems.EMBER_METAL_INGOT, 5)
            .fluid(ModBlocks.FIRE_CAULDRON.get())
            .consume(1000)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_ingot_2"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 3)
            .transform(ModBlocks.FIRE_CAULDRON.get())
            .result(ModItems.EMBER_METAL_INGOT, 6)
            .fluid(ModBlocks.FIRE_CAULDRON.get())
            .consume(1000)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_ingot_3"));

        TimeWarpRecipe.builder()
            .requires(Items.SKELETON_SKULL)
            .requires(Items.COAL, 4)
            .result(Items.WITHER_SKELETON_SKULL)
            .save(provider);

        TimeWarpRecipe.builder()
            .requires(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
            .consume(1000)
            .fluid(Blocks.POWDER_SNOW_CAULDRON)
            .result(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE)
            .save(provider);

        TimeWarpRecipe.builder()
            .requires(ModItemTags.TITANIUM_INGOTS)
            .requires(ModItemTags.SILVER_INGOTS)
            .requires(Items.IRON_INGOT)
            .consume(1000)
            .fluid(Blocks.POWDER_SNOW_CAULDRON)
            .result(ModItems.FROST_METAL_INGOT)
            .save(provider);

        TimeWarpRecipe.builder()
            .requires(ItemTags.FLOWERS)
            .result(Items.WITHER_ROSE, 0.2f)
            .save(provider);

        TimeWarpRecipe.builder()
            .heat(HeatTier.INCANDESCENT, 6000)
            .requires(ModBlocks.URANIUM_BLOCK)
            .result(ModItems.RAW_URANIUM, 2)
            .result(ModItems.RAW_LEAD.asStack())
            .save(provider, AnvilCraft.of("time_warp/raw_uranium_from_uranium_block"));
        TimeWarpRecipe.builder()
            .heat(HeatTier.INCANDESCENT, 12000)
            .requires(ModBlocks.PLUTONIUM_BLOCK)
            .result(ModItems.RAW_URANIUM, 3)
            .result(ModItems.RAW_LEAD.asStack())
            .save(provider, AnvilCraft.of("time_warp/raw_uranium_from_plutonium_block"));


        TimeWarpRecipe.builder()
            .requires(ItemIngredientPredicate.Builder.item()
                .of(ModBlocks.RESIN_BLOCK)
                .withSubPredicate(
                    LibItemSubPredicates.NOT.get(),
                    NotPredicate.of(ModItemSubPredicates.SAVED_ENTITY.get(), ItemSavedEntityPredicate.any())
                )
                .build()
            )
            .result(ModBlocks.AMBER_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RESIN_BLOCK), AnvilCraftDatagen.has(ModBlocks.RESIN_BLOCK))
            .save(provider, AnvilCraft.of("time_warp/amber_block"));

        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .hasCauldron(0, -1, 0)
            .hasBlock(builder -> builder
                .of(ModBlocks.CORRUPTED_BEACON.get())
                .with(CorruptedBeaconBlock.LIT, true)
                .offset(0, -2, 0)
            )
            .hasItemIngredient(builder -> builder
                .of(ModBlocks.RESIN_BLOCK)
                .offset(0.0, -0.375, 0.0)
                .range(0.75, 0.75, 0.75)
                .with(
                    LibItemSubPredicates.NOT.get(),
                    NotPredicate.of(ModItemSubPredicates.SAVED_ENTITY.get(), ItemSavedEntityPredicate.monster())
                )
                .saveComponent(ModComponents.SAVED_ENTITY, AnvilCraft.of("saved_entity"))
            )
            .spawnItem(builder -> builder
                .item(ModBlocks.MOB_AMBER_BLOCK)
                .offset(0.0, -0.75, 0.0)
                .applyComponent(ModComponents.SAVED_ENTITY, AnvilCraft.of("saved_entity"))
            )
            .maxEfficiency(1)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RESIN_BLOCK), AnvilCraftDatagen.has(ModBlocks.RESIN_BLOCK))
            .group("time_warp")
            .icon(ModBlocks.MOB_AMBER_BLOCK.asStack())
            .save(provider, AnvilCraft.of("mob_amber_block"));

        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .hasCauldron(0, -1, 0)
            .hasBlock(builder -> builder
                .of(ModBlocks.CORRUPTED_BEACON.get())
                .with(CorruptedBeaconBlock.LIT, true)
                .offset(0, -2, 0)
            )
            .hasItemIngredient(builder -> builder
                .of(ModBlocks.RESIN_BLOCK)
                .offset(0.0, -0.375, 0.0)
                .range(0.75, 0.75, 0.75)
                .with(
                    ModItemSubPredicates.SAVED_ENTITY.get(),
                    ItemSavedEntityPredicate.monster()
                )
                .saveComponent(ModComponents.SAVED_ENTITY, AnvilCraft.of("saved_entity"))
            )
            .chooseOne(builder -> builder
                .choice(
                    SpawnItem.builder().item(ModBlocks.MOB_AMBER_BLOCK)
                        .offset(0.0, -0.75, 0.0)
                        .applyComponent(ModComponents.SAVED_ENTITY, AnvilCraft.of("saved_entity"))
                        .build(),
                    19
                )
                .choice(
                    SpawnItem.builder().item(ModBlocks.RESENTFUL_AMBER_BLOCK)
                        .offset(0.0, -0.75, 0.0)
                        .applyComponent(ModComponents.SAVED_ENTITY, AnvilCraft.of("saved_entity"))
                        .build(),
                    1
                )
            )
            .maxEfficiency(1)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RESIN_BLOCK), AnvilCraftDatagen.has(ModBlocks.RESIN_BLOCK))
            .group("time_warp")
            .icon(ModBlocks.RESENTFUL_AMBER_BLOCK.asStack())
            .save(provider, AnvilCraft.of("resentful_amber_block"));
    }

    private static void timeWarp(
        RegistrateRecipeProvider provider,
        ItemLike input,
        int inputCount,
        ItemLike output,
        int outputCount
    ) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .result(output, outputCount)
            .save(provider);
    }

    @SuppressWarnings("SameParameterValue")
    private static void timeWarp(
        RegistrateRecipeProvider provider,
        TagKey<Item> input,
        int inputCount,
        ItemLike output,
        int outputCount
    ) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .result(output, outputCount)
            .save(provider);
    }

    private static void timeWarpToOilCauldron(RegistrateRecipeProvider provider, ItemLike input, int inputCount) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .transform(ModBlocks.OIL_CAULDRON.get())
            .produce(250)
            .save(
                provider,
                AnvilCraft.of("time_warp/oil_from_" + BuiltInRegistries.ITEM.getKey(input.asItem()).getPath())
            );
    }

    @SuppressWarnings("SameParameterValue")
    private static void timeWarpToOilCauldron(RegistrateRecipeProvider provider, TagKey<Item> input, int inputCount) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .transform(ModBlocks.OIL_CAULDRON.get())
            .produce(250)
            .save(
                provider,
                AnvilCraft.of("time_warp/oil_from_" + input.location().getPath())
            );
    }
}
