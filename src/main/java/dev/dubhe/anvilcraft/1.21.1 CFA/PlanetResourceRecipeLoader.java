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

import java.util.List;
import java.util.Optional;

/**
 * Data generation loader for planet resource recipes.
 */
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

    // === Mineral ===

    private static void createMineralRecipe(RegistrumRecipeProvider provider) {
        saveRecipe(provider, "mineral", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.MINERAL,
            Optional.of(new PlanetResourceRecipe.MineralData(
                "c:raw_materials", "anvilcraft:non_planetary_minerals", 10
            )),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        ));
    }

    // === Fluids ===

    private static void createFluidRecipes(RegistrumRecipeProvider provider) {
        saveRecipe(provider, "fluid_water", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.FLUID,
            Optional.empty(),
            Optional.of(new PlanetResourceRecipe.FluidData(
                "rocky_planet", "", "low", "minecraft:water"
            )),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        ));

        saveRecipe(provider, "fluid_lava", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.FLUID,
            Optional.empty(),
            Optional.of(new PlanetResourceRecipe.FluidData(
                "rocky_planet", "scorched", "low", "minecraft:lava"
            )),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        ));
    }

    // === Giant planet items ===

    private static void createGiantItemRecipes(RegistrumRecipeProvider provider) {
        saveRecipe(provider, "giant_item_ice", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.GIANT_ITEM,
            Optional.empty(), Optional.empty(),
            Optional.of(new PlanetResourceRecipe.GiantData(
                List.of(
                    new PlanetResourceRecipe.WeightedEntry("minecraft:ice", 50),
                    new PlanetResourceRecipe.WeightedEntry("minecraft:packed_ice", 30),
                    new PlanetResourceRecipe.WeightedEntry("minecraft:blue_ice", 20)
                ), "ice"
            )),
            Optional.empty(), Optional.empty(), Optional.empty()
        ));
    }

    // === Giant planet fluids ===

    private static void createGiantFluidRecipes(RegistrumRecipeProvider provider) {
        saveRecipe(provider, "giant_fluid_gas", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.GIANT_FLUID,
            Optional.empty(), Optional.empty(),
            Optional.of(new PlanetResourceRecipe.GiantData(
                List.of(
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:hydrogen", 90),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:helium", 10)
                ), "gas"
            )),
            Optional.empty(), Optional.empty(), Optional.empty()
        ));

        saveRecipe(provider, "giant_fluid_ice", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.GIANT_FLUID,
            Optional.empty(), Optional.empty(),
            Optional.of(new PlanetResourceRecipe.GiantData(
                List.of(
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:hydrogen", 80),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:helium", 10),
                    new PlanetResourceRecipe.WeightedEntry("minecraft:water", 10)
                ), "ice"
            )),
            Optional.empty(), Optional.empty(), Optional.empty()
        ));
    }

    // === Biological ===

    private static void createBiologicalRecipe(RegistrumRecipeProvider provider) {
        saveRecipe(provider, "biological", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.BIOLOGICAL,
            Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(new PlanetResourceRecipe.BiologicalData(
                PlanetResourceRecipe.LifeChances.DEFAULT,
                "anvilcraft:planetary_land_animals",
                "anvilcraft:planetary_aquatic_animals",
                "anvilcraft:non_planetary_mob_drops",
                List.of(
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:milk", 50),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:honey", 50)
                )
            )),
            Optional.empty(), Optional.empty()
        ));
    }

    // === Offering ===

    private static void createOfferingRecipe(RegistrumRecipeProvider provider) {
        saveRecipe(provider, "offering", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.OFFERING,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(new PlanetResourceRecipe.OfferingData(
                List.of(
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:gem_block_random", 50),
                    new PlanetResourceRecipe.WeightedEntry("minecraft:experience_bottle", 40),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:royal_steel_ingot", 5),
                    new PlanetResourceRecipe.WeightedEntry("minecraft:totem_of_undying", 2),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:gem_amulet_random", 2),
                    new PlanetResourceRecipe.WeightedEntry("minecraft:heart_of_the_sea", 1)
                ), 50, 32, 43
            )),
            Optional.empty()
        ));
    }

    // === Wasteland ===

    private static void createWastelandRecipe(RegistrumRecipeProvider provider) {
        saveRecipe(provider, "wasteland", new PlanetResourceRecipe(
            PlanetResourceRecipe.Category.WASTELAND,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(new PlanetResourceRecipe.WastelandData(
                List.of(
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:reinforced_concrete_gray", 60),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:circuit_board", 30),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:processor", 5),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:raw_uranium", 3),
                    new PlanetResourceRecipe.WeightedEntry("anvilcraft:plutonium_nugget", 2)
                ), 35, 10
            ))
        ));
    }
}
