package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.data.ItemEnchantmentsData;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.BiFunction;

public class PermutationRecipeLoader {
    static final List<String> WEAPONS_AND_TOOLS = List.of(
        "sword",
        "axe",
        "pickaxe",
        "shovel",
        "hoe"
    );
    static final List<String> ANC_WEAPONS_AND_TOOLS = List.of(
        "heavy_halberd",
        "resonator"
    );
    private static final List<String> WORKSTATIONS = List.of(
        "anvil",
        "grindstone",
        "smithing_table"
    );

    public static void init(RegistrateRecipeProvider provider) {
        PermutationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ModItems.ROYAL_STEEL_INGOT,
            ResourceLocation.withDefaultNamespace("diamond"),
            AnvilCraft.of("royal_steel")
        );

        PermutationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ModItems.EMBER_METAL_INGOT,
            ResourceLocation.withDefaultNamespace("netherite"),
            AnvilCraft.of("ember_metal"),
            (netherite, ember) -> PermutationRecipe.builder().input(
                RecipeResult.builder()
                    .result(netherite)
                    .removeData(ModComponents.FIRE_REFORGING)
            ).input(
                RecipeResult.builder()
                    .result(ember)
            )
        );

        PermutationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WEAPONS_AND_TOOLS,
            ModItems.MULTIPHASE_MATTER,
            AnvilCraft.of("frost_metal"),
            AnvilCraft.of("ember_metal"),
            (frost, ember) -> PermutationRecipe.builder().input(
                RecipeResult.builder()
                    .result(frost)
                    .removeData(ModComponents.FIRE_REFORGING)
            ).input(
                RecipeResult.builder()
                    .result(ember)
                    .removeData(ModComponents.MERCILESS)
                    .changeDataType(RecipeInputSlot.input(0), ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantmentsData.enchantments(0))
                    .removeAttribute(Merciless.MERCILESS_ID)
            )
        );
        PermutationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.ANC_WEAPONS_AND_TOOLS,
            ModItems.MULTIPHASE_MATTER,
            AnvilCraft.of("frost_metal"),
            AnvilCraft.of("ember_metal"),
            (frost, ember) -> PermutationRecipe.builder().input(
                RecipeResult.builder()
                    .result(frost)
                    .removeData(ModComponents.FIRE_REFORGING)
            ).input(
                RecipeResult.builder()
                    .result(ember)
                    .removeData(ModComponents.MERCILESS)
                    .changeDataType(RecipeInputSlot.input(0), ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantmentsData.enchantments(0))
                    .removeAttribute(Merciless.MERCILESS_ID)
            )
        );

        PermutationRecipeLoader.register(
            provider,
            PermutationRecipeLoader.WORKSTATIONS,
            ModBlocks.MULTIPHASE_MATTER_BLOCK,
            AnvilCraft.of("frost"),
            AnvilCraft.of("ember")
        );

        PermutationRecipe.builder()
            .material(ModItems.MULTIPHASE_MATTER)
            .input(ModItems.EMERALD_AMULET)
            .input(ModItems.TOPAZ_AMULET)
            .input(ModItems.RUBY_AMULET)
            .input(ModItems.SAPPHIRE_AMULET)
            .save(provider, "gem_amulets");
    }

    private static void register(
        RegistrateRecipeProvider provider,
        List<String> bases,
        ItemLike material,
        ResourceLocation idA,
        ResourceLocation idB
    ) {
        for (String base : bases) {
            Item inputA = BuiltInRegistries.ITEM.get(idA.withSuffix("_" + base));
            Item inputB = BuiltInRegistries.ITEM.get(idB.withSuffix("_" + base));
            PermutationRecipe.builder()
                .material(material)
                .input(inputA)
                .input(inputB)
                .save(provider, PermutationRecipeLoader.defaultId(inputA, inputB));
        }
    }

    private static void register(
        RegistrateRecipeProvider provider,
        List<String> bases,
        ItemLike material,
        ResourceLocation idA,
        ResourceLocation idB,
        BiFunction<Item, Item, PermutationRecipe.Builder> builderFactory
    ) {
        for (String base : bases) {
            Item inputA = BuiltInRegistries.ITEM.get(idA.withSuffix("_" + base));
            Item inputB = BuiltInRegistries.ITEM.get(idB.withSuffix("_" + base));
            builderFactory.apply(inputA, inputB)
                .material(material)
                .save(provider, PermutationRecipeLoader.defaultId(inputA, inputB));
        }
    }

    private static String defaultId(Item inputA, Item inputB) {
        ResourceLocation inputAId = BuiltInRegistries.ITEM.getKey(inputA);
        String inputBPath = BuiltInRegistries.ITEM.getKey(inputB).getPath();
        return inputAId.withSuffix("_and_" + inputBPath).getPath();
    }
}
