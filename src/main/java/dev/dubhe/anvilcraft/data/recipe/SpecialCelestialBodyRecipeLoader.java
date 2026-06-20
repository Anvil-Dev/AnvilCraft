package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public class SpecialCelestialBodyRecipeLoader {

    public static void init(RegistrumRecipeProvider provider) {
        createOverworldLike(provider);
        createFleshPlanet(provider);
        createIntelligentPlanet(provider);
        createShatteredPlanet(provider);
        createHollowPlanet(provider);
        createErrorPlanet(provider);
    }

    private static void saveRecipe(RecipeOutput output, String name, SpecialCelestialBodyRecipe recipe) {
        ResourceLocation id = AnvilCraft.of("special_celestial_body/" + name);
        Advancement.Builder advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        output.accept(id, recipe, advancement.build(id.withPrefix("recipes/")));
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation anvil(String path) {
        return ResourceLocation.fromNamespaceAndPath("anvilcraft", path);
    }

    private static SpecialCelestialBodyRecipe.WeightedEntry item(String id, int weight) {
        return new SpecialCelestialBodyRecipe.WeightedEntry(id, weight);
    }

    private static void createOverworldLike(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider, "overworld_like", new SpecialCelestialBodyRecipe(
                "overworld_like",
                "planet_overworld",
                false,
                32,
                14,
                20,
                16,
                true,
                Optional.of(LiquidCoverage.MEDIUM),
                2,
                2,
                0f,
                List.of(
                    mc("grass_block"),
                    mc("podzol"),
                    mc("mycelium"),
                    mc("dirt"),
                    mc("coarse_dirt"),
                    mc("rooted_dirt"),
                    mc("moss_block"),
                    mc("mud")
                ),
                List.of(item("minecraft:raw_copper", 50), item("minecraft:raw_iron", 30), item("minecraft:raw_gold", 20)),
                List.of(item("minecraft:water", 100)),
                List.of(
                    item("minecraft:porkchop", 5),
                    item("minecraft:beef", 5),
                    item("minecraft:mutton", 5),
                    item("minecraft:chicken", 5),
                    item("minecraft:leather", 5),
                    item("minecraft:feather", 5),
                    item("minecraft:white_wool", 10),
                    item("minecraft:light_gray_wool", 4),
                    item("minecraft:gray_wool", 4),
                    item("minecraft:black_wool", 4),
                    item("minecraft:brown_wool", 4),
                    item("minecraft:red_wool", 2),
                    item("minecraft:orange_wool", 2),
                    item("minecraft:yellow_wool", 2),
                    item("minecraft:lime_wool", 2),
                    item("minecraft:green_wool", 2),
                    item("minecraft:cyan_wool", 2),
                    item("minecraft:light_blue_wool", 2),
                    item("minecraft:blue_wool", 2),
                    item("minecraft:purple_wool", 2),
                    item("minecraft:magenta_wool", 2),
                    item("minecraft:pink_wool", 2)
                ),
                List.of(),
                List.of(),
                List.of()
            )
        );
    }

    private static void createFleshPlanet(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider, "flesh_planet", new SpecialCelestialBodyRecipe(
                "flesh_planet",
                "planet_flesh",
                true,
                40,
                10,
                9,
                17,
                true,
                Optional.of(LiquidCoverage.NONE),
                2,
                2,
                22f,
                List.of(anvil("rotten_flesh_block"), mc("bone_block")),
                List.of(
                    item("minecraft:rotten_flesh", 30),
                    item("minecraft:bone", 30),
                    item("minecraft:string", 20),
                    item("minecraft:spider_eye", 15),
                    item("minecraft:phantom_membrane", 3),
                    item("minecraft:ghast_tear", 2)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );
    }

    private static void createIntelligentPlanet(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider, "intelligent_planet", new SpecialCelestialBodyRecipe(
                "intelligent_planet",
                "planet_intelligence",
                true,
                58,
                12,
                12,
                18,
                false,
                Optional.of(LiquidCoverage.HIGH),
                1,
                2,
                2.71828f,
                List.of(anvil("spacetime_supercomputer")),
                List.of(),
                List.of(item("anvilcraft:exp_fluid", 100)),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );
    }

    private static void createShatteredPlanet(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider, "shattered_planet", new SpecialCelestialBodyRecipe(
                "shattered_planet",
                "planet_shattered",
                true,
                13,
                9,
                13,
                27,
                false,
                Optional.of(LiquidCoverage.MEDIUM),
                0,
                4,
                0f,
                List.of(mc("magma_block"), mc("netherrack"), mc("blackstone"), mc("basalt"), anvil("earth_core_shard_block")),
                List.of(
                    item("anvilcraft:raw_tungsten", 30),
                    item("minecraft:raw_gold", 30),
                    item("anvilcraft:raw_silver", 30),
                    item("anvilcraft:earth_core_shard", 10)
                ),
                List.of(item("minecraft:lava", 100)),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );
    }

    private static void createHollowPlanet(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider, "hollow_planet", new SpecialCelestialBodyRecipe(
                "hollow_planet",
                "planet_hollow",
                true,
                60,
                10,
                1,
                17,
                false,
                Optional.of(LiquidCoverage.NONE),
                3,
                4,
                45f,
                List.of(anvil("negative_matter_block")),
                List.of(item("minecraft:obsidian", 90), item("anvilcraft:void_matter", 8), item("anvilcraft:negative_matter_nugget", 2)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );
    }

    private static void createErrorPlanet(RegistrumRecipeProvider provider) {
        saveRecipe(
            provider, "error_planet", new SpecialCelestialBodyRecipe(
                "error_planet",
                "planet_error",
                true,
                64,
                64,
                64,
                64,
                false,
                Optional.of(LiquidCoverage.NONE),
                -1,
                0,
                0f,
                List.of(anvil("creative_generator"), mc("command_block"), mc("structure_block")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );
    }
}
