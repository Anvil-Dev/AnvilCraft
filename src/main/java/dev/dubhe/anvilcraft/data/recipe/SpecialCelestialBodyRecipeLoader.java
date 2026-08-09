package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Optional;

public class SpecialCelestialBodyRecipeLoader {

    public static void init(RegistrumRecipeProvider provider) {
        SpecialCelestialBodyRecipeLoader.createOverworldLike(provider);
        SpecialCelestialBodyRecipeLoader.createFleshPlanet(provider);
        SpecialCelestialBodyRecipeLoader.createIntelligentPlanet(provider);
        SpecialCelestialBodyRecipeLoader.createHollowPlanet(provider);
        SpecialCelestialBodyRecipeLoader.createErrorPlanet(provider);
    }

    private static void saveRecipe(RecipeOutput output, String name, SpecialCelestialBodyRecipe recipe) {
        Identifier id = AnvilCraft.of("special_celestial_body/" + name);
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        Advancement.Builder advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(key))
            .rewards(AdvancementRewards.Builder.recipe(key))
            .requirements(AdvancementRequirements.Strategy.OR);
        output.accept(key, recipe, advancement.build(id.withPrefix("recipes/")));
    }

    private static Identifier mc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    private static Identifier anvil(String path) {
        return Identifier.fromNamespaceAndPath("anvilcraft", path);
    }

    private static SpecialCelestialBodyRecipe.WeightedEntry item(String id, int weight) {
        return new SpecialCelestialBodyRecipe.WeightedEntry(id, weight);
    }

    private static void createOverworldLike(RegistrumRecipeProvider provider) {
        SpecialCelestialBodyRecipeLoader.saveRecipe(provider, "overworld_like", new SpecialCelestialBodyRecipe(
            "overworld_like", "planet_overworld", false,
            32, 14, 20, 16, true, Optional.of(LiquidCoverage.MEDIUM), 2, 2, 0f,
            List.of(SpecialCelestialBodyRecipeLoader.mc("grass_block")),
            List.of(
                SpecialCelestialBodyRecipeLoader.item("minecraft:raw_copper", 50),
                SpecialCelestialBodyRecipeLoader.item("minecraft:raw_iron", 30),
                SpecialCelestialBodyRecipeLoader.item("minecraft:raw_gold", 20)
            ),
            List.of(SpecialCelestialBodyRecipeLoader.item("minecraft:water", 100)),
            List.of(
                SpecialCelestialBodyRecipeLoader.item("minecraft:porkchop", 5),
                SpecialCelestialBodyRecipeLoader.item("minecraft:beef", 5),
                SpecialCelestialBodyRecipeLoader.item("minecraft:mutton", 5),
                SpecialCelestialBodyRecipeLoader.item("minecraft:chicken", 5),
                SpecialCelestialBodyRecipeLoader.item("minecraft:leather", 5),
                SpecialCelestialBodyRecipeLoader.item("minecraft:feather", 5),
                SpecialCelestialBodyRecipeLoader.item("minecraft:white_wool", 10),
                SpecialCelestialBodyRecipeLoader.item("minecraft:light_gray_wool", 4),
                SpecialCelestialBodyRecipeLoader.item("minecraft:gray_wool", 4),
                SpecialCelestialBodyRecipeLoader.item("minecraft:black_wool", 4),
                SpecialCelestialBodyRecipeLoader.item("minecraft:brown_wool", 4),
                SpecialCelestialBodyRecipeLoader.item("minecraft:red_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:orange_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:yellow_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:lime_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:green_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:cyan_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:light_blue_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:blue_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:purple_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:magenta_wool", 2),
                SpecialCelestialBodyRecipeLoader.item("minecraft:pink_wool", 2)
            ),
            List.of(), List.of(), List.of()
        ));
    }

    private static void createFleshPlanet(RegistrumRecipeProvider provider) {
        SpecialCelestialBodyRecipeLoader.saveRecipe(provider, "flesh_planet", new SpecialCelestialBodyRecipe(
            "flesh_planet", "planet_flesh", true,
            40, 10, 9, 17, true, Optional.of(LiquidCoverage.NONE), 2, 2, 22f,
            List.of(SpecialCelestialBodyRecipeLoader.anvil("rotten_flesh_block")),
            List.of(
                SpecialCelestialBodyRecipeLoader.item("minecraft:rotten_flesh", 30),
                SpecialCelestialBodyRecipeLoader.item("minecraft:bone", 30),
                SpecialCelestialBodyRecipeLoader.item("minecraft:string", 20),
                SpecialCelestialBodyRecipeLoader.item("minecraft:spider_eye", 15),
                SpecialCelestialBodyRecipeLoader.item("minecraft:phantom_membrane", 3),
                SpecialCelestialBodyRecipeLoader.item("minecraft:ghast_tear", 2)
            ),
            List.of(), List.of(), List.of(), List.of(), List.of()
        ));
    }

    private static void createIntelligentPlanet(RegistrumRecipeProvider provider) {
        SpecialCelestialBodyRecipeLoader.saveRecipe(provider, "intelligent_planet", new SpecialCelestialBodyRecipe(
            "intelligent_planet", "planet_intelligence", true,
            58, 12, 12, 18, false, Optional.of(LiquidCoverage.HIGH), 1, 2, 2.71828f,
            List.of(SpecialCelestialBodyRecipeLoader.anvil("spacetime_supercomputer")),
            List.of(), List.of(SpecialCelestialBodyRecipeLoader.item("anvilcraft:exp_fluid", 100)),
            List.of(), List.of(), List.of(), List.of()
        ));
    }

    private static void createHollowPlanet(RegistrumRecipeProvider provider) {
        SpecialCelestialBodyRecipeLoader.saveRecipe(provider, "hollow_planet", new SpecialCelestialBodyRecipe(
            "hollow_planet", "planet_hollow", true,
            60, 10, 1, 17, false, Optional.of(LiquidCoverage.NONE), 3, 4, 45f,
            List.of(SpecialCelestialBodyRecipeLoader.anvil("negative_matter_block")),
            List.of(
                SpecialCelestialBodyRecipeLoader.item("minecraft:obsidian", 90),
                SpecialCelestialBodyRecipeLoader.item("anvilcraft:void_matter", 8),
                SpecialCelestialBodyRecipeLoader.item("anvilcraft:negative_matter_nugget", 2)
            ),
            List.of(), List.of(), List.of(), List.of(), List.of()
        ));
    }

    private static void createErrorPlanet(RegistrumRecipeProvider provider) {
        SpecialCelestialBodyRecipeLoader.saveRecipe(provider, "error_planet", new SpecialCelestialBodyRecipe(
            "error_planet", "planet_error", true,
            64, 64, 64, 64, false, Optional.of(LiquidCoverage.NONE), -1, 0, 0f,
            List.of(SpecialCelestialBodyRecipeLoader.anvil("creative_generator")),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));
    }
}
