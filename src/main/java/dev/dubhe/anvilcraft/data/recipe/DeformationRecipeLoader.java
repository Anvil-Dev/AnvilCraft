package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

    public static void init(RegistrateRecipeProvider provider) {
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ResourceLocation.withDefaultNamespace("wooden"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ResourceLocation.withDefaultNamespace("stone"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ResourceLocation.withDefaultNamespace("iron"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ResourceLocation.withDefaultNamespace("golden"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ResourceLocation.withDefaultNamespace("diamond"),
            "weapons_and_tools"
        );
        DeformationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ResourceLocation.withDefaultNamespace("netherite"),
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
            ResourceLocation.withDefaultNamespace("chainmail"),
            "armors"
        );
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            ResourceLocation.withDefaultNamespace("iron"),
            "armors"
        );
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            ResourceLocation.withDefaultNamespace("golden"),
            "armors"
        );
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            ResourceLocation.withDefaultNamespace("diamond"),
            "armors"
        );
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.ARMORS,
            ResourceLocation.withDefaultNamespace("netherite"),
            "armors"
        );

        DeformationRecipe.builder()
            .input(Items.BOW)
            .input(Items.CROSSBOW)
            .save(provider, "bowlikes");
    }

    private static void register(
        RegistrateRecipeProvider provider,
        List<String> bases,
        ResourceLocation id,
        String suffix
    ) {
        var builder = DeformationRecipe.builder();
        for (String base : bases) {
            Item input = BuiltInRegistries.ITEM.get(id.withSuffix("_" + base));
            builder.input(RecipeResult.simple(input));
        }
        builder.save(provider, id.withSuffix("_" + suffix).getPath());
    }
}
