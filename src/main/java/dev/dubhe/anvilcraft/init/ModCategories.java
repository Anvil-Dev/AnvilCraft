package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.crate.category.AndCategory;
import dev.dubhe.anvilcraft.api.crate.category.CreativeModeTabCategory;
import dev.dubhe.anvilcraft.api.crate.category.DataComponentCategory;
import dev.dubhe.anvilcraft.api.crate.category.FilterCategory;
import dev.dubhe.anvilcraft.api.crate.category.HasDataComponentCategory;
import dev.dubhe.anvilcraft.api.crate.category.ICategory;
import dev.dubhe.anvilcraft.api.crate.category.ItemClassCategory;
import dev.dubhe.anvilcraft.api.crate.category.MaxStackSizeCategory;
import dev.dubhe.anvilcraft.api.crate.category.ModCategory;
import dev.dubhe.anvilcraft.api.crate.category.client.RecipeBookCategoryCategory;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class ModCategories {
    private static final DeferredRegister<ICategory.Type<?>> REGISTER = DeferredRegister.create(
        ModRegistries.CATEGORY_TYPE_KEY,
        AnvilCraft.MOD_ID
    );
    public static final DeferredHolder<ICategory.Type<?>, ModCategory.Type> MOD = REGISTER.register(
        "mod",
        ModCategory.Type::new
    );
    public static final DeferredHolder<ICategory.Type<?>, ItemClassCategory.Type> ITEM_CLASS = REGISTER.register(
        "item_class",
        ItemClassCategory.Type::new
    );
    public static final DeferredHolder<ICategory.Type<?>, MaxStackSizeCategory.Type> MAX_STACK_SIZE = REGISTER.register(
        "max_stack_size",
        MaxStackSizeCategory.Type::new
    );
    public static final DeferredHolder<ICategory.Type<?>, HasDataComponentCategory.Type> HAS_DATA_COMPONENT = REGISTER.register(
        "has_data_component",
        HasDataComponentCategory.Type::new
    );
    public static final DeferredHolder<ICategory.Type<?>, DataComponentCategory.Type> DATA_COMPONENT = REGISTER.register(
        "data_component",
        DataComponentCategory.Type::new
    );
    public static final DeferredHolder<ICategory.Type<?>, CreativeModeTabCategory.Type> CREATIVE_MODE_TAB = REGISTER.register(
        "creative_mode_tab",
        CreativeModeTabCategory.Type::new
    );
    public static final DeferredHolder<ICategory.Type<?>, FilterCategory.Type> FILTER = REGISTER.register(
        "filter",
        FilterCategory.Type::new
    );
    public static final DeferredHolder<ICategory.Type<?>, AndCategory.Type> AND = REGISTER.register(
        "and",
        AndCategory.Type::new
    );
    public static final DeferredHolder<ICategory.Type<?>, RecipeBookCategoryCategory.Type> RECIPE_BOOK_CATEGORY = REGISTER.register(
        "recipe_book_category",
        RecipeBookCategoryCategory.Type::new
    );

    public static final ResourceKey<ICategory> MINECRAFT = key("minecraft");
    public static final ResourceKey<ICategory> BLOCK = key("block");
    public static final ResourceKey<ICategory> UNSTACKABLE = key("unstackable");
    public static final ResourceKey<ICategory> FOOD = key("food");
    public static final ResourceKey<ICategory> ANVILCRAFT = key("anvilcraft");
    public static final ResourceKey<ICategory> REDSTONE = key("redstone");
    public static final ResourceKey<ICategory> ENCHANTED = key("enchanted");

    public static void bootstrap(BootstrapContext<ICategory> ctx) {
        ctx.register(MINECRAFT, new ModCategory(Items.GRASS_BLOCK, "minecraft"));
        ctx.register(BLOCK, new ItemClassCategory(Items.BRICKS, "block", BlockItem.class));
        ctx.register(UNSTACKABLE, new MaxStackSizeCategory(Items.DIAMOND_SWORD, "unstackable", MinMaxBounds.Ints.exactly(1)));
        ctx.register(FOOD, new HasDataComponentCategory(Items.BAKED_POTATO, "food", DataComponents.FOOD));
        ctx.register(ANVILCRAFT, new ModCategory(ModBlocks.ROYAL_ANVIL, "anvilcraft"));
        ctx.register(REDSTONE, new AndCategory(
            Items.REDSTONE,
            "redstone",
            new CreativeModeTabCategory(Items.REDSTONE, "redstone_tab", CreativeModeTabs.REDSTONE_BLOCKS),
            new RecipeBookCategoryCategory(Items.REDSTONE, "redstone_recipe_book", "crafting_redstone")
        ));
        ctx.register(ENCHANTED, new HasDataComponentCategory(
            Items.ENCHANTED_BOOK,
            "enchanted",
            DataComponents.ENCHANTMENTS,
            DataComponents.STORED_ENCHANTMENTS
        ));
    }

    private static ResourceKey<ICategory> key(String path) {
        return ResourceKey.create(ModRegistries.CATEGORY_KEY, AnvilCraft.of(path));
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
