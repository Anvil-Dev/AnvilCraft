package dev.dubhe.anvilcraft.init.storage;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.saved.storage.category.BlockCategory;
import dev.dubhe.anvilcraft.saved.storage.category.CreativeModeTabCategory;
import dev.dubhe.anvilcraft.saved.storage.category.HasComponentCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.NamespaceCategory;
import dev.dubhe.anvilcraft.saved.storage.category.OrCategory;
import dev.dubhe.anvilcraft.saved.storage.category.UnstackableCategory;
import dev.dubhe.anvilcraft.saved.storage.category.client.RecipeBookCategoryCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

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
            new NamespaceCategory(Blocks.GRASS_BLOCK, ResourceLocation.DEFAULT_NAMESPACE)
        );
        ctx.register(ModCategories.BLOCK, BlockCategory.INSTANCE);
        ctx.register(ModCategories.UNSTACKABLE, UnstackableCategory.INSTANCE);
        ctx.register(
            ModCategories.FOOD_AND_DRINK,
            HasComponentCategory.or(
                Items.APPLE,
                ModCategories.FOOD_AND_DRINK.location(),
                DataComponents.FOOD,
                DataComponents.POTION_CONTENTS
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
                ModCategories.REDSTONE.location(),
                new CreativeModeTabCategory(
                    Items.REDSTONE,
                    ModCategories.REDSTONE.location().withSuffix("_tab"),
                    CreativeModeTabs.REDSTONE_BLOCKS
                ),
                new RecipeBookCategoryCategory(
                    Items.REDSTONE,
                    ModCategories.REDSTONE.location().withSuffix("_recipe_book"),
                    "crafting_redstone"
                )
            )
        );
        ctx.register(
            ModCategories.ENCHANTED,
            HasComponentCategory.or(
                Items.ENCHANTED_BOOK,
                ModCategories.ENCHANTED.location(),
                DataComponents.ENCHANTMENTS,
                DataComponents.STORED_ENCHANTMENTS,
                ModComponents.MERCILESS_ENCHANTMENTS,
                ModComponents.DISABLED_ENCHANTMENTS
            )
        );
    }

    private static ResourceKey<ICategory> key(String name) {
        return ResourceKey.create(ModRegistryKeys.CATEGORY, AnvilCraft.of(name));
    }
}
