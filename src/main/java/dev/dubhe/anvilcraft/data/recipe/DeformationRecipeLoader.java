package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public class DeformationRecipeLoader {
    private static final List<String> ARMORS = List.of(
        "helmet",
        "chestplate",
        "leggings",
        "boots"
    );

    public static void init(RegistrumRecipeProvider provider) {
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            Identifier.withDefaultNamespace("wooden"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            Identifier.withDefaultNamespace("stone"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            Identifier.withDefaultNamespace("iron"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            Identifier.withDefaultNamespace("golden"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            Identifier.withDefaultNamespace("diamond"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            Identifier.withDefaultNamespace("netherite"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            AnvilCraft.of("amethyst"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            AnvilCraft.of("royal_steel"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            AnvilCraft.of("frost_metal"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            AnvilCraft.of("ember_metal"),
            "weapons_and_tools"
        );

        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            Identifier.withDefaultNamespace("chainmail"),
            "armors"
        );
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            Identifier.withDefaultNamespace("iron"),
            "armors"
        );
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            Identifier.withDefaultNamespace("golden"),
            "armors"
        );
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            Identifier.withDefaultNamespace("diamond"),
            "armors"
        );
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            Identifier.withDefaultNamespace("netherite"),
            "armors"
        );

        DeformationRecipe.builder()
            .input(Items.BOW)
            .input(Items.CROSSBOW)
            .save(provider, "bowlikes");
    }

    private static void register(
        RegistrumRecipeProvider provider,
        List<String> bases,
        Identifier id,
        String suffix
    ) {
        var builder = DeformationRecipe.builder();
        for (String base : bases) {
            Item input = BuiltInRegistries.ITEM.getValue(id.withSuffix("_" + base));
            builder.input(RecipeResult.simple(input));
        }
        builder.save(provider, id.withSuffix("_" + suffix).getPath());
    }
}
