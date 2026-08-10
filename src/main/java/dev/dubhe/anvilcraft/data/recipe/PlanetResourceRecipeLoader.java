package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetResourceRecipe;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Data generation loader for planet resource recipes. */
public class PlanetResourceRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        PlanetResourceRecipeLoader.createMineralRecipe(provider);
        PlanetResourceRecipeLoader.createFluidRecipes(provider);
        PlanetResourceRecipeLoader.createGiantItemRecipes(provider);
        PlanetResourceRecipeLoader.createGiantFluidRecipes(provider);
        PlanetResourceRecipeLoader.createBiologicalRecipe(provider);
        PlanetResourceRecipeLoader.createOfferingRecipe(provider);
        PlanetResourceRecipeLoader.createWastelandRecipe(provider);
    }

    private static void saveRecipe(RecipeOutput output, String name, PlanetResourceRecipe recipe) {
        Identifier id = AnvilCraft.of("planet_resource/" + name);
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        Advancement.Builder advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(key))
            .rewards(AdvancementRewards.Builder.recipe(key))
            .requirements(AdvancementRequirements.Strategy.OR);
        output.accept(key, recipe, advancement.build(id.withPrefix("recipes/")));
    }

    private static void createMineralRecipe(RegistrumRecipeProvider provider) {
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "mineral",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.MINERAL)
                .mineral(new PlanetResourceRecipe.MineralData(
                    "c:raw_materials",
                    "anvilcraft:non_planetary_minerals",
                    10
                ))
                .build()
        );
    }

    private static void createFluidRecipes(RegistrumRecipeProvider provider) {
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "fluid_water",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.FLUID)
                .fluid(new PlanetResourceRecipe.FluidData("rocky_planet", "", "low", Fluids.WATER))
                .build()
        );
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "fluid_lava",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.FLUID)
                .fluid(new PlanetResourceRecipe.FluidData("rocky_planet", "scorched", "low", Fluids.LAVA))
                .build()
        );
    }

    private static void createGiantItemRecipes(RegistrumRecipeProvider provider) {
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "giant_item_ice",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.GIANT_ITEM)
                .giant(new PlanetResourceRecipe.GiantData(
                    PlanetResourceRecipe.entries(entries -> entries
                        .item(Items.ICE, 50)
                        .item(Items.PACKED_ICE, 30)
                        .item(Items.BLUE_ICE, 20)),
                    "ice"
                ))
                .build()
        );
    }

    private static void createGiantFluidRecipes(RegistrumRecipeProvider provider) {
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "giant_fluid_gas",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.GIANT_FLUID)
                .giant(new PlanetResourceRecipe.GiantData(
                    PlanetResourceRecipe.entries(entries -> entries.fluid(ModFluids.PRIMORDIAL_MATTER, 100)),
                    "gas"
                ))
                .build()
        );
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "giant_fluid_ice",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.GIANT_FLUID)
                .giant(new PlanetResourceRecipe.GiantData(
                    PlanetResourceRecipe.entries(entries -> entries
                        .fluid(ModFluids.PRIMORDIAL_MATTER, 90)
                        .fluid(Fluids.WATER, 10)),
                    "ice"
                ))
                .build()
        );
    }

    private static void createBiologicalRecipe(RegistrumRecipeProvider provider) {
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "biological",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.BIOLOGICAL)
                .biological(new PlanetResourceRecipe.BiologicalData(
                    PlanetResourceRecipe.LifeChances.DEFAULT,
                    "anvilcraft:planetary_land_animals",
                    "anvilcraft:planetary_aquatic_animals",
                    "anvilcraft:non_planetary_mob_drops",
                    PlanetResourceRecipe.entries(entries -> entries
                        .fluid(NeoForgeMod.MILK, 50)
                        .fluid(ModFluids.HONEY, 50))
                ))
                .build()
        );
    }

    private static void createOfferingRecipe(RegistrumRecipeProvider provider) {
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "offering",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.OFFERING)
                .offering(new PlanetResourceRecipe.OfferingData(
                    PlanetResourceRecipe.entries(entries -> entries
                        .chooseOne(50, choices -> choices
                            .item(Items.EMERALD_BLOCK, 1)
                            .item(ModBlocks.TOPAZ_BLOCK.asItem(), 1)
                            .item(ModBlocks.RUBY_BLOCK.asItem(), 1)
                            .item(ModBlocks.SAPPHIRE_BLOCK.asItem(), 1))
                        .item(Items.EXPERIENCE_BOTTLE, 40)
                        .item(ModItems.ROYAL_STEEL_INGOT, 5)
                        .item(Items.TOTEM_OF_UNDYING, 2)
                        .chooseOne(2, choices -> choices
                            .item(ModItems.EMERALD_AMULET, 1)
                            .item(ModItems.TOPAZ_AMULET, 1)
                            .item(ModItems.RUBY_AMULET, 1)
                            .item(ModItems.SAPPHIRE_AMULET, 1))
                        .item(Items.HEART_OF_THE_SEA, 1)),
                    50,
                    32,
                    43
                ))
                .build()
        );
    }

    private static void createWastelandRecipe(RegistrumRecipeProvider provider) {
        PlanetResourceRecipeLoader.saveRecipe(
            provider,
            "wasteland",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.WASTELAND)
                .wasteland(new PlanetResourceRecipe.WastelandData(
                    PlanetResourceRecipe.entries(entries -> entries
                        .item(ModBlocks.REINFORCED_CONCRETES.get(Color.GRAY).asItem(), 60)
                        .item(ModItems.CIRCUIT_BOARD, 30)
                        .item(ModItems.PROCESSOR, 5)
                        .item(ModItems.RAW_URANIUM, 3)
                        .item(ModItems.PLUTONIUM_NUGGET, 2)),
                    35,
                    10
                ))
                .build()
        );
    }
}
