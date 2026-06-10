package dev.dubhe.anvilcraft.init.storage;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.saved.storage.category.CreativeModeTabCategory;
import dev.dubhe.anvilcraft.saved.storage.category.HasComponentCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.NamespaceCategory;
import dev.dubhe.anvilcraft.saved.storage.category.OrCategory;
import dev.dubhe.anvilcraft.saved.storage.category.client.RecipeBookCategoryCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.holdersets.AnyHolderSet;

import java.util.List;
import java.util.Map;

public class ModCategories {
    public static final ResourceKey<ICategory> FOODS_AND_DRINKS = ModCategories.key("foods_and_drinks");
    public static final ResourceKey<ICategory> ANVILCRAFT = ModCategories.key("anvilcraft");
    public static final ResourceKey<ICategory> REDSTONE = ModCategories.key("redstone");
    public static final ResourceKey<ICategory> ENCHANTED = ModCategories.key("enchanted");

    public static void bootstrap(BootstrapContext<ICategory> ctx) {
        ctx.register(
            ModCategories.FOODS_AND_DRINKS,
            HasComponentCategory.or(
                Items.APPLE,
                ModCategories.FOODS_AND_DRINKS.identifier(),
                Map.of(
                    DataComponentPredicate.AnyValueType.create(DataComponents.FOOD),
                    EnchantmentsPredicate.enchantments(List.of()), // ignored
                    DataComponentPredicates.POTIONS,
                    PotionsPredicate.potions(new AnyHolderSet<>(Registries.POTION, ctx.holderLookup(Registries.POTION).orElseThrow()))
                )
            )
        );
        ctx.register(
            ModCategories.ANVILCRAFT,
            new NamespaceCategory(ModBlocks.ROYAL_ANVIL, AnvilCraft.MOD_ID)
        );
        ctx.register(
            ModCategories.REDSTONE,
            new OrCategory(
                Items.REDSTONE,
                ModCategories.REDSTONE.identifier(),
                new CreativeModeTabCategory(
                    Items.REDSTONE,
                    ModCategories.REDSTONE.identifier().withSuffix("_tab"),
                    CreativeModeTabs.REDSTONE_BLOCKS
                ),
                new RecipeBookCategoryCategory(
                    Items.REDSTONE,
                    ModCategories.REDSTONE.identifier().withSuffix("_recipe_book"),
                    ResourceKey.create(Registries.RECIPE_BOOK_CATEGORY, Identifier.withDefaultNamespace("crafting_redstone"))
                )
            )
        );
        ctx.register(
            ModCategories.ENCHANTED,
            HasComponentCategory.or(
                Items.ENCHANTED_BOOK,
                ModCategories.ENCHANTED.identifier(),
                Map.of(
                    DataComponentPredicates.ENCHANTMENTS,
                    EnchantmentsPredicate.enchantments(List.of()),
                    DataComponentPredicates.STORED_ENCHANTMENTS,
                    EnchantmentsPredicate.storedEnchantments(List.of()),
                    DataComponentPredicate.AnyValueType.create(ModComponents.MERCILESS_ENCHANTMENTS),
                    EnchantmentsPredicate.enchantments(List.of()), // ignored
                    DataComponentPredicate.AnyValueType.create(ModComponents.DISABLED_ENCHANTMENTS),
                    EnchantmentsPredicate.enchantments(List.of()) // ignored
                )
            )
        );
    }

    private static ResourceKey<ICategory> key(String name) {
        return ResourceKey.create(ModRegistryKeys.CATEGORY, AnvilCraft.of(name));
    }
}
