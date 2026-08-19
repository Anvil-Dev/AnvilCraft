package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetResourceRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

/** Data generation loader for planet resource recipes. */
public class PlanetResourceRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        createMineralRecipe(provider);
        createFluidRecipes(provider);
        createGiantItemRecipes(provider);
        createGiantFluidRecipes(provider);
        createBiologicalRecipe(provider);
        createOfferingRecipe(provider);
        createWastelandRecipe(provider);
    }

    private static void saveRecipe(RecipeOutput output, String name, PlanetResourceRecipe recipe) {
        ResourceLocation id = AnvilCraft.of("planet_resource/" + name);
        Advancement.Builder advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        output.accept(id, recipe, advancement.build(id.withPrefix("recipes/")));
    }

    private static void createMineralRecipe(RegistrumRecipeProvider provider) {
        saveRecipe(
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
        saveRecipe(
            provider,
            "fluid_water",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.FLUID)
                .fluid(new PlanetResourceRecipe.FluidData("rocky_planet", "", "low", "minecraft:water"))
                .build()
        );
        saveRecipe(
            provider,
            "fluid_lava",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.FLUID)
                .fluid(new PlanetResourceRecipe.FluidData("rocky_planet", "scorched", "low", "minecraft:lava"))
                .build()
        );
    }

    private static void createGiantItemRecipes(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider,
            "giant_item_ice",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.GIANT_ITEM)
                .giant(new PlanetResourceRecipe.GiantData(
                    PlanetResourceRecipe.entries(entries -> entries
                        .id("minecraft:ice", 50)
                        .id("minecraft:packed_ice", 30)
                        .id("minecraft:blue_ice", 20)),
                    "ice"
                ))
                .build()
        );
    }

    private static void createGiantFluidRecipes(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider,
            "giant_fluid_gas",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.GIANT_FLUID)
                .giant(new PlanetResourceRecipe.GiantData(
                    PlanetResourceRecipe.entries(entries -> entries.id("anvilcraft:primordial_matter", 100)),
                    "gas"
                ))
                .build()
        );
        saveRecipe(
            provider,
            "giant_fluid_ice",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.GIANT_FLUID)
                .giant(new PlanetResourceRecipe.GiantData(
                    PlanetResourceRecipe.entries(entries -> entries
                        .id("anvilcraft:primordial_matter", 90)
                        .id("minecraft:water", 10)),
                    "ice"
                ))
                .build()
        );
    }

    private static void createBiologicalRecipe(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider,
            "biological",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.BIOLOGICAL)
                .biological(new PlanetResourceRecipe.BiologicalData(
                    PlanetResourceRecipe.LifeChances.DEFAULT,
                    "anvilcraft:planetary_land_animals",
                    "anvilcraft:planetary_aquatic_animals",
                    "anvilcraft:non_planetary_mob_drops",
                    PlanetResourceRecipe.entries(entries -> entries
                        .id("minecraft:milk", 50)
                        .id("anvilcraft:honey", 50))
                ))
                .build()
        );
    }

    private static void createOfferingRecipe(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider,
            "offering",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.OFFERING)
                .offering(new PlanetResourceRecipe.OfferingData(
                    PlanetResourceRecipe.entries(entries -> entries
                        .chooseOne(50, choices -> choices
                            .id("minecraft:emerald_block", 1)
                            .id("anvilcraft:topaz_block", 1)
                            .id("anvilcraft:ruby_block", 1)
                            .id("anvilcraft:sapphire_block", 1))
                        .id("minecraft:experience_bottle", 40)
                        .id("anvilcraft:royal_steel_ingot", 5)
                        .id("minecraft:totem_of_undying", 2)
                        .chooseOne(2, choices -> choices
                            .id("anvilcraft:emerald_amulet", 1)
                            .id("anvilcraft:topaz_amulet", 1)
                            .id("anvilcraft:ruby_amulet", 1)
                            .id("anvilcraft:sapphire_amulet", 1))
                        .id("minecraft:heart_of_the_sea", 1)),
                    50,
                    32,
                    43
                ))
                .build()
        );
    }

    private static void createWastelandRecipe(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider,
            "wasteland",
            PlanetResourceRecipe.builder(PlanetResourceRecipe.Category.WASTELAND)
                .wasteland(new PlanetResourceRecipe.WastelandData(
                    PlanetResourceRecipe.entries(entries -> entries
                        .id("anvilcraft:reinforced_concrete_gray", 60)
                        .id("anvilcraft:circuit_board", 30)
                        .id("anvilcraft:processor", 5)
                        .id("anvilcraft:raw_uranium", 3)
                        .id("anvilcraft:plutonium_nugget", 2)),
                    35,
                    10
                ))
                .build()
        );
    }
}
