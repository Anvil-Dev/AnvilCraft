package dev.dubhe.anvilcraft.init.storage;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModDataComponentPredicates;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.item.property.predicate.ExtraEnchantmentsPredicate;
import dev.dubhe.anvilcraft.saved.storage.category.BlockCategory;
import dev.dubhe.anvilcraft.saved.storage.category.CreativeModeTabCategory;
import dev.dubhe.anvilcraft.saved.storage.category.HasComponentCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.NamespaceCategory;
import dev.dubhe.anvilcraft.saved.storage.category.OrCategory;
import dev.dubhe.anvilcraft.saved.storage.category.UnstackableCategory;
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
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.holdersets.AnyHolderSet;

import java.util.List;
import java.util.Map;

public class ModCategories {
    public static final ResourceKey<ICategory> MINECRAFT = ModCategories.key("minecraft");
    public static final ResourceKey<ICategory> BLOCK = ModCategories.key("block");
    public static final ResourceKey<ICategory> UNSTACKABLE = ModCategories.key("unstackable");
    public static final ResourceKey<ICategory> FOOD_AND_DRINK = ModCategories.key("food_and_drink");
    public static final ResourceKey<ICategory> ANVILCRAFT = ModCategories.key("anvilcraft");
    public static final ResourceKey<ICategory> REDSTONE = ModCategories.key("redstone");
    public static final ResourceKey<ICategory> ENCHANTED = ModCategories.key("enchanted");

    public static void bootstrap(BootstrapContext<ICategory> ctx) {
        ctx.register(
            ModCategories.MINECRAFT,
            new NamespaceCategory(Blocks.GRASS_BLOCK, Identifier.DEFAULT_NAMESPACE)
        );
        ctx.register(ModCategories.BLOCK, BlockCategory.INSTANCE);
        ctx.register(ModCategories.UNSTACKABLE, UnstackableCategory.INSTANCE);
        ctx.register(
            ModCategories.FOOD_AND_DRINK,
            HasComponentCategory.or(
                Items.APPLE,
                ModCategories.FOOD_AND_DRINK.identifier(),
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
                    ModDataComponentPredicates.MERCILESS_ENCH.get(),
                    ExtraEnchantmentsPredicate.merciless(List.of()),
                    ModDataComponentPredicates.DISABLED_ENCH.get(),
                    ExtraEnchantmentsPredicate.disabled(List.of())
                )
            )
        );
    }

    private static ResourceKey<ICategory> key(String name) {
        return ResourceKey.create(ModRegistryKeys.CATEGORY, AnvilCraft.of(name));
    }
}
