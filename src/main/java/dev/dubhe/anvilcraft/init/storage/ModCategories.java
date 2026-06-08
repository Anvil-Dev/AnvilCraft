package dev.dubhe.anvilcraft.init.storage;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.saved.storage.category.CreativeModeTabCategory;
import dev.dubhe.anvilcraft.saved.storage.category.HasComponentCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.NamespaceCategory;
import dev.dubhe.anvilcraft.saved.storage.category.OrCategory;
import dev.dubhe.anvilcraft.saved.storage.category.client.RecipeBookCategoryCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.AnyValue;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

public class ModCategories {
    public static final ResourceKey<ICategory> FOODS_AND_DRINKS = ModCategories.key("foods_and_drinks");
    public static final ResourceKey<ICategory> ANVILCRAFT = ModCategories.key("anvilcraft");
    public static final ResourceKey<ICategory> REDSTONE = ModCategories.key("redstone");
    public static final ResourceKey<ICategory> ENCHANTED = ModCategories.key("enchanted");

    public static void bootstrap(BootstrapContext<ICategory> ctx) {
        ctx.register(
            ModCategories.FOODS_AND_DRINKS,
            new OrCategory(
                Items.APPLE,
                ModCategories.FOODS_AND_DRINKS.identifier(),
                new HasComponentCategory(
                    Items.APPLE,
                    AnvilCraft.of("foods"),
                    DataComponentPredicate.AnyValueType.create(DataComponents.FOOD),
                    new AnyValue(DataComponents.FOOD)
                ),
                new HasComponentCategory(
                    Items.POTION,
                    AnvilCraft.of("drinks"),
                    DataComponentPredicate.AnyValueType.create(DataComponents.POTION_CONTENTS),
                    new AnyValue(DataComponents.POTION_CONTENTS)
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
            new OrCategory(
                Items.ENCHANTED_BOOK,
                ModCategories.ENCHANTED.identifier(),
                new HasComponentCategory(
                    Items.ENCHANTED_BOOK,
                    ModCategories.ENCHANTED.identifier().withSuffix(".enchantments"),
                    DataComponentPredicate.AnyValueType.create(DataComponents.ENCHANTMENTS),
                    new AnyValue(DataComponents.ENCHANTMENTS)
                ),
                new HasComponentCategory(
                    Items.ENCHANTED_BOOK,
                    ModCategories.ENCHANTED.identifier().withSuffix(".stored_enchantments"),
                    DataComponentPredicate.AnyValueType.create(DataComponents.STORED_ENCHANTMENTS),
                    new AnyValue(DataComponents.STORED_ENCHANTMENTS)
                ),
                new HasComponentCategory(
                    ModItems.FROST_METAL_SWORD,
                    ModCategories.ENCHANTED.identifier().withSuffix(".merciless_enchantments"),
                    DataComponentPredicate.AnyValueType.create(ModComponents.MERCILESS_ENCHANTMENTS),
                    new AnyValue(ModComponents.MERCILESS_ENCHANTMENTS)
                ),
                new HasComponentCategory(
                    Items.BARRIER,
                    ModCategories.ENCHANTED.identifier().withSuffix(".disabled_enchantments"),
                    DataComponentPredicate.AnyValueType.create(ModComponents.DISABLED_ENCHANTMENTS),
                    new AnyValue(ModComponents.DISABLED_ENCHANTMENTS)
                )
            )
        );
    }

    private static ResourceKey<ICategory> key(String name) {
        return ResourceKey.create(ModRegistryKeys.CATEGORY, AnvilCraft.of(name));
    }
}
