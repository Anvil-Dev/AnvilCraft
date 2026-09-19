package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.data.ItemEnchantmentsData;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.recipe.frost.CustomFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public class PermutationRecipeLoader {
    static final List<String> WEAPONS_AND_TOOLS = List.of(
        "sword",
        "axe",
        "pickaxe",
        "shovel",
        "hoe"
    );
    private static final List<String> WORKSTATIONS = List.of(
        "anvil",
        "grindstone",
        "smithing_table"
    );
    private static final ResourceLocation DIAMOND = ResourceLocation.withDefaultNamespace("diamond");
    private static final ResourceLocation IRON = ResourceLocation.withDefaultNamespace("iron");
    private static final ResourceLocation GOLDEN = ResourceLocation.withDefaultNamespace("golden");
    private static final ResourceLocation NETHERITE = ResourceLocation.withDefaultNamespace("netherite");
    private static final ResourceLocation AMETHYST = AnvilCraft.of("amethyst");
    private static final ResourceLocation ROYAL_STEEL = AnvilCraft.of("royal_steel");
    private static final ResourceLocation FROST_METAL = AnvilCraft.of("frost_metal");
    private static final ResourceLocation EMBER_METAL = AnvilCraft.of("ember_metal");

    public static void init(RegistrumRecipeProvider provider) {
        PermutationRecipeLoader.registerWeaponsAndTools(provider);
        PermutationRecipeLoader.registerHeavyItems(provider);

        PermutationRecipe.builder()
            .input(
                ModItems.SPECTRAL_WEAPON_LAUNCHER,
                ModItems.ANVIL_RAILGUN,
                ModItems.CORRUPTED_BEACON_ACTIVATOR,
                ModItems.TESLA_GUN,
                ModItems.LASER_GUN
            )
            .options(
                CustomFrostMaterialPredicate.of(PermutationRecipeLoader.count(ModItems.TRANSCENDIUM_INGOT, 2)),
                ModItems.SPECTRAL_WEAPON_LAUNCHER,
                ModItems.ANVIL_RAILGUN,
                ModItems.CORRUPTED_BEACON_ACTIVATOR,
                ModItems.TESLA_GUN,
                ModItems.LASER_GUN
            )
            .save(provider, "energy_weapons");

        PermutationRecipe.builder()
            .input(ModItems.EMERALD_AMULET, ModItems.TOPAZ_AMULET, ModItems.RUBY_AMULET, ModItems.SAPPHIRE_AMULET)
            .options(
                CustomFrostMaterialPredicate.of(PermutationRecipeLoader.count(ModBlocks.CHROMATIC_STONE, 1)),
                ModItems.EMERALD_AMULET,
                ModItems.TOPAZ_AMULET,
                ModItems.RUBY_AMULET,
                ModItems.SAPPHIRE_AMULET
            )
            .save(provider, "gem_amulets");

        PermutationRecipe.builder()
            .input(ModItems.SILENCE_AMULET, ModItems.ARMADILLO_AMULET, ModItems.CAT_AMULET, ModItems.DOG_AMULET)
            .options(
                CustomFrostMaterialPredicate.of(PermutationRecipeLoader.count(Items.CAKE, 1)),
                ModItems.SILENCE_AMULET,
                ModItems.ARMADILLO_AMULET,
                ModItems.CAT_AMULET,
                ModItems.DOG_AMULET
            )
            .save(provider, "nature_amulets");
    }

    private static void registerWeaponsAndTools(RegistrumRecipeProvider provider) {
        for (String base : PermutationRecipeLoader.WEAPONS_AND_TOOLS) {
            Item diamond = PermutationRecipeLoader.item(PermutationRecipeLoader.DIAMOND, base);
            Item iron = PermutationRecipeLoader.item(PermutationRecipeLoader.IRON, base);
            Item golden = PermutationRecipeLoader.item(PermutationRecipeLoader.GOLDEN, base);
            Item amethyst = PermutationRecipeLoader.item(PermutationRecipeLoader.AMETHYST, base);
            Item netherite = PermutationRecipeLoader.item(PermutationRecipeLoader.NETHERITE, base);
            Item royalSteel = PermutationRecipeLoader.item(PermutationRecipeLoader.ROYAL_STEEL, base);
            Item frostMetal = PermutationRecipeLoader.item(PermutationRecipeLoader.FROST_METAL, base);
            Item emberMetal = PermutationRecipeLoader.item(PermutationRecipeLoader.EMBER_METAL, base);

            PermutationRecipe.builder()
                .input(royalSteel)
                .result(diamond)
                .result(iron)
                .result(golden)
                .result(amethyst)
                .save(provider, PermutationRecipeLoader.id(royalSteel));

            PermutationRecipe.builder()
                .input(netherite)
                .result(diamond)
                .save(provider, PermutationRecipeLoader.id(netherite));

            PermutationRecipe.builder()
                .input(emberMetal)
                .result(RecipeResult.builder().result(royalSteel).removeData(ModComponents.FIRE_REFORGING).build())
                .result(RecipeResult.builder().result(netherite).removeData(ModComponents.FIRE_REFORGING).build())
                .option(
                    CustomFrostMaterialPredicate.of(PermutationRecipeLoader.count(ModItems.FROST_METAL_NUGGET, 3)),
                    RecipeResult.builder().result(frostMetal).removeData(ModComponents.FIRE_REFORGING).build()
                )
                .save(provider, PermutationRecipeLoader.id(emberMetal));

            PermutationRecipe.builder()
                .input(frostMetal)
                .result(RecipeResult.simple(royalSteel).build())
                .option(
                    CustomFrostMaterialPredicate.of(PermutationRecipeLoader.count(ModItems.EMBER_METAL_NUGGET, 3)),
                    PermutationRecipeLoader.merciless(emberMetal)
                )
                .save(provider, PermutationRecipeLoader.id(frostMetal));
        }
    }

    private static void registerHeavyItems(RegistrumRecipeProvider provider) {
        for (String base : PermutationRecipeLoader.WORKSTATIONS) {
            PermutationRecipeLoader.registerHeavyPair(
                provider,
                PermutationRecipeLoader.item(AnvilCraft.of("frost"), base),
                PermutationRecipeLoader.item(AnvilCraft.of("ember"), base),
                false
            );
        }
        PermutationRecipeLoader.registerHeavyPair(
            provider,
            ModItems.FROST_ANVIL_HAMMER,
            ModItems.EMBER_ANVIL_HAMMER,
            true
        );
        PermutationRecipeLoader.registerHeavyPair(
            provider,
            ModItems.FROST_DRAGON_ROD,
            ModItems.EMBER_DRAGON_ROD,
            true
        );
        PermutationRecipeLoader.registerHeavyPair(
            provider,
            ModItems.FROST_METAL_RESONATOR,
            ModItems.EMBER_METAL_RESONATOR,
            true
        );
        PermutationRecipeLoader.registerHeavyPair(
            provider,
            ModItems.FROST_METAL_HEAVY_HALBERD,
            ModItems.EMBER_METAL_HEAVY_HALBERD,
            true
        );
    }

    /**
     * 生成一对浮霜与余烬重型物品之间的嬗变配方。
     *
     * @param reforged 余烬物品是否带有火炼与无情数据，工作方块类重型物品没有这些数据
     */
    private static void registerHeavyPair(
        RegistrumRecipeProvider provider,
        ItemLike frost,
        ItemLike ember,
        boolean reforged
    ) {
        RecipeResult.Builder frostResult = RecipeResult.simple(frost);
        if (reforged) frostResult.removeData(ModComponents.FIRE_REFORGING);
        PermutationRecipe.builder()
            .input(ember)
            .option(
                CustomFrostMaterialPredicate.of(PermutationRecipeLoader.count(ModItems.FROST_METAL_INGOT, 3)),
                frostResult.build()
            )
            .save(provider, PermutationRecipeLoader.id(ember.asItem()));
        PermutationRecipe.builder()
            .input(frost)
            .option(
                CustomFrostMaterialPredicate.of(PermutationRecipeLoader.count(ModItems.EMBER_METAL_INGOT, 3)),
                reforged ? PermutationRecipeLoader.merciless(ember) : RecipeResult.simple(ember).build()
            )
            .save(provider, PermutationRecipeLoader.id(frost.asItem()));
    }

    private static RecipeResult merciless(ItemLike result) {
        return RecipeResult.builder()
            .result(result)
            .removeData(ModComponents.MERCILESS)
            .changeDataType(
                RecipeInputSlot.input(0),
                ModComponents.MERCILESS_ENCHANTMENTS,
                ItemEnchantmentsData.enchantments(0)
            )
            .removeAttribute(Merciless.MERCILESS_ID)
            .build();
    }

    private static ItemIngredientPredicate count(ItemLike item, int count) {
        return ItemIngredientPredicate.of(item).withCount(count).build();
    }

    private static Item item(ResourceLocation prefix, String base) {
        return BuiltInRegistries.ITEM.get(prefix.withSuffix("_" + base));
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
