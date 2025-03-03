package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.TimeWarpRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

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
        timeWarp(provider, ModBlocks.URANIUM_BLOCK, 1, ModItems.RAW_URANIUM, 3);
        timeWarp(provider, ModItems.SEA_HEART_SHELL_SHARD, 1, ModItems.SEA_HEART_SHELL, 1);

        TimeWarpRecipe.builder()
            .requires(Items.EMERALD)
            .result(new ItemStack(Items.EMERALD_BLOCK))
            .consumeFluid(true)
            .cauldron(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItems.RUBY)
            .result(new ItemStack(ModBlocks.RUBY_BLOCK))
            .consumeFluid(true)
            .cauldron(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItems.TOPAZ)
            .result(new ItemStack(ModBlocks.TOPAZ_BLOCK))
            .consumeFluid(true)
            .cauldron(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ModItems.SAPPHIRE)
            .result(new ItemStack(ModBlocks.SAPPHIRE_BLOCK))
            .consumeFluid(true)
            .cauldron(ModBlocks.MELT_GEM_CAULDRON.get())
            .save(provider);
        TimeWarpRecipe.builder()
            .requires(ItemTags.LOGS)
            .result(new ItemStack(Items.COAL))
            .save(provider, AnvilCraft.of("time_warp/coal_from_logs"));

        timeWarpToOilCauldron(provider, Items.ROTTEN_FLESH, 64);
        timeWarpToOilCauldron(provider, Items.SPIDER_EYE, 64);
        timeWarpToOilCauldron(provider, Items.CHICKEN, 64);
        timeWarpToOilCauldron(provider, Tags.Items.FOODS_RAW_FISH, 64);
        timeWarpToOilCauldron(provider, Items.BEEF, 16);
        timeWarpToOilCauldron(provider, Items.PORKCHOP, 16);
        timeWarpToOilCauldron(provider, Items.MUTTON, 16);
        timeWarpToOilCauldron(provider, Items.RABBIT, 16);

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .cauldron(ModBlocks.FIRE_CAULDRON.get())
            .results(
                List.of(
                    ChanceItemStack.of(ModItems.EMBER_METAL_NUGGET.asStack(4)),
                    ChanceItemStack.of(ModItems.EMBER_METAL_NUGGET.asStack(4)).withChance(0.50f),
                    ChanceItemStack.of(ModItems.EMBER_METAL_NUGGET.asStack(4)).withChance(0.25f)
                )
            )
            .requiredFluidLevel(CauldronUtil.maxLevel(ModBlocks.FIRE_CAULDRON.get()))
            .consumeFluid(true)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_nugget_0"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 1)
            .cauldron(ModBlocks.FIRE_CAULDRON.get())
            .results(
                List.of(
                    ChanceItemStack.of(ModItems.EMBER_METAL_NUGGET.asStack(9))
                )
            )
            .requiredFluidLevel(CauldronUtil.maxLevel(ModBlocks.FIRE_CAULDRON.get()))
            .consumeFluid(true)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_nugget_1"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 2)
            .cauldron(ModBlocks.FIRE_CAULDRON.get())
            .results(
                List.of(
                    ChanceItemStack.of(ModItems.EMBER_METAL_NUGGET.asStack(12))
                )
            )
            .requiredFluidLevel(CauldronUtil.maxLevel(ModBlocks.FIRE_CAULDRON.get()))
            .consumeFluid(true)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_nugget_2"));

        TimeWarpRecipe.builder()
            .requires(ModItemTags.NETHERITE_BLOCK)
            .requires(ModItems.EARTH_CORE_SHARD, 3)
            .cauldron(ModBlocks.FIRE_CAULDRON.get())
            .results(
                List.of(
                    ChanceItemStack.of(ModItems.EMBER_METAL_NUGGET.asStack(15))
                )
            )
            .requiredFluidLevel(CauldronUtil.maxLevel(ModBlocks.FIRE_CAULDRON.get()))
            .consumeFluid(true)
            .save(provider, AnvilCraft.of("time_warp/ember_metal_nugget_3"));
    }

    private static void timeWarp(
        RegistrateRecipeProvider provider, ItemLike input, int inputCount, ItemLike output, int outputCount) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .result(new ItemStack(output, outputCount))
            .save(provider);
    }

    private static void timeWarpToOilCauldron(RegistrateRecipeProvider provider, ItemLike input, int inputCount) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .produceFluid(true)
            .cauldron(ModBlocks.OIL_CAULDRON.get())
            .save(
                provider,
                AnvilCraft.of("time_warp/oil_from_"
                    + BuiltInRegistries.ITEM.getKey(input.asItem()).getPath()));
    }

    private static void timeWarpToOilCauldron(RegistrateRecipeProvider provider, TagKey<Item> input, int inputCount) {
        TimeWarpRecipe.builder()
            .requires(input, inputCount)
            .produceFluid(true)
            .cauldron(ModBlocks.OIL_CAULDRON.get())
            .save(
                provider,
                AnvilCraft.of("time_warp/oil_from_" + input.location().getPath()));
    }
}
