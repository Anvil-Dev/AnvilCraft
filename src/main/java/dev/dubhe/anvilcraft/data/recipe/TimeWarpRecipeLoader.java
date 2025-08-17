package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
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
        timeWarp(provider, ModBlocks.ZINC_BLOCK, 1, ModItems.RAW_ZINC, 3);
        timeWarp(provider, ModBlocks.TIN_BLOCK, 1, ModItems.RAW_TIN, 3);
        timeWarp(provider, ModBlocks.TITANIUM_BLOCK, 1, ModItems.RAW_TITANIUM, 3);
        timeWarp(provider, ModBlocks.TUNGSTEN_BLOCK, 1, ModItems.RAW_TUNGSTEN, 3);
        timeWarp(provider, ModBlocks.LEAD_BLOCK, 1, ModItems.RAW_LEAD, 3);
        timeWarp(provider, ModBlocks.SILVER_BLOCK, 1, ModItems.RAW_SILVER, 3);
        timeWarp(provider, ModItems.SEA_HEART_SHELL_SHARD, 1, ModItems.SEA_HEART_SHELL, 1);

        TimeWarpRecipe.builder()
            .requires(Items.EMERALD)
            .result(Items.EMERALD_BLOCK)
            .consume(1)
            .transform(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItems.RUBY)
            .result(ModBlocks.RUBY_BLOCK)
            .consume(1)
            .transform(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItems.TOPAZ)
            .result(ModBlocks.TOPAZ_BLOCK)
            .consume(1)
            .transform(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItems.SAPPHIRE)
            .result(ModBlocks.SAPPHIRE_BLOCK)
            .consume(1)
            .transform(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ItemTags.LOGS)
            .result(Items.COAL)
            .save(provider, AnvilCraft.of("time_warp/coal_from_logs"));

        timeWarpToOilCauldron(provider, Items.ROTTEN_FLESH, 64);
        timeWarpToOilCauldron(provider, Items.SPIDER_EYE, 64);
        timeWarpToOilCauldron(provider, Items.CHICKEN, 64);
        timeWarpToOilCauldron(provider, Tags.Items.FOODS_RAW_FISH, 64);
        timeWarpToOilCauldron(provider, Items.BEEF, 16);
        timeWarpToOilCauldron(provider, Items.PORKCHOP, 16);
        timeWarpToOilCauldron(provider, Items.MUTTON, 16);
        timeWarpToOilCauldron(provider, Items.RABBIT, 16);

        timeWarpToOilCauldron(provider, Items.ZOMBIE_HEAD, 1);
        timeWarpToOilCauldron(provider, Items.PIGLIN_HEAD, 1);

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .transform(ModBlocks.FIRE_CAULDRON.get())
            .result(ModItems.EMBER_METAL_INGOT, 3)
            .fluid(ModBlocks.FIRE_CAULDRON.get())
            .consume(1)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_ingot_0"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 1)
            .transform(ModBlocks.FIRE_CAULDRON.get())
            .result(ModItems.EMBER_METAL_INGOT, 4)
            .fluid(ModBlocks.FIRE_CAULDRON.get())
            .consume(1)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_ingot_1"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 2)
            .transform(ModBlocks.FIRE_CAULDRON.get())
            .result(ModItems.EMBER_METAL_INGOT, 5)
            .fluid(ModBlocks.FIRE_CAULDRON.get())
            .consume(1)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_ingot_2"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 3)
            .transform(ModBlocks.FIRE_CAULDRON.get())
            .result(ModItems.EMBER_METAL_INGOT, 6)
            .fluid(ModBlocks.FIRE_CAULDRON.get())
            .consume(1)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_ingot_3"));

        TimeWarpRecipe.builder()
            .requires(Items.SKELETON_SKULL)
            .requires(Items.COAL, 4)
            .result(Items.WITHER_SKELETON_SKULL)
            .save(provider);

        TimeWarpRecipe.builder()
            .requires(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
            .consume(3)
            .fluid(Blocks.POWDER_SNOW_CAULDRON)
            .result(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE)
            .save(provider);

        TimeWarpRecipe.builder()
            .requires(ModItems.TITANIUM_INGOT)
            .requires(ModItems.SILVER_INGOT)
            .requires(Items.IRON_INGOT)
            .consume(3)
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
    }

    private static void timeWarp(
        RegistrateRecipeProvider provider, ItemLike input, int inputCount, ItemLike output, int outputCount) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .result(output, outputCount)
            .save(provider);
    }

    private static void timeWarpToOilCauldron(RegistrateRecipeProvider provider, ItemLike input, int inputCount) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .consume(-1)
            .transform(ModBlocks.OIL_CAULDRON.get())
            .save(
                provider,
                AnvilCraft.of("time_warp/oil_from_"
                    + BuiltInRegistries.ITEM.getKey(input.asItem()).getPath()));
    }

    @SuppressWarnings("SameParameterValue")
    private static void timeWarpToOilCauldron(RegistrateRecipeProvider provider, TagKey<Item> input, int inputCount) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .consume(-1)
            .transform(ModBlocks.OIL_CAULDRON.get())
            .save(
                provider,
                AnvilCraft.of("time_warp/oil_from_" + input.location().getPath()));
    }
}
