package dev.dubhe.anvilcraft.init.storage;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.saved.storage.category.AndCategory;
import dev.dubhe.anvilcraft.saved.storage.category.BlockCategory;
import dev.dubhe.anvilcraft.saved.storage.category.CreativeModeTabCategory;
import dev.dubhe.anvilcraft.saved.storage.category.FilterCategory;
import dev.dubhe.anvilcraft.saved.storage.category.HasComponentCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.NamespaceCategory;
import dev.dubhe.anvilcraft.saved.storage.category.OrCategory;
import dev.dubhe.anvilcraft.saved.storage.category.UnstackableCategory;
import dev.dubhe.anvilcraft.saved.storage.category.client.RecipeBookCategoryCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCategoryTypes {
    private static final DeferredRegister<ICategory.Type<?>> REGISTER = DeferredRegister.create(
        ModRegistryKeys.CATEGORY_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<ICategory.Type<?>, BlockCategory.Type> BLOCK = REGISTER
        .register("block", BlockCategory.Type::new);
    public static final DeferredHolder<ICategory.Type<?>, UnstackableCategory.Type> UNSTACKABLE = REGISTER
        .register("unstackable", UnstackableCategory.Type::new);
    public static final DeferredHolder<ICategory.Type<?>, AndCategory.Type> AND = REGISTER.register("and", AndCategory.Type::new);
    public static final DeferredHolder<ICategory.Type<?>, OrCategory.Type> OR = REGISTER.register("or", OrCategory.Type::new);
    public static final DeferredHolder<ICategory.Type<?>, HasComponentCategory.Type> HAS_COMPONENT = REGISTER
        .register("has_component", HasComponentCategory.Type::new);
    public static final DeferredHolder<ICategory.Type<?>, NamespaceCategory.Type> NAMESPACE = REGISTER
        .register("namespace", NamespaceCategory.Type::new);
    public static final DeferredHolder<ICategory.Type<?>, CreativeModeTabCategory.Type> CREATIVE_MODE_TAB = REGISTER
        .register("creative_mode_tab", CreativeModeTabCategory.Type::new);
    public static final DeferredHolder<ICategory.Type<?>, FilterCategory.Type> FILTER = REGISTER
        .register("filter", FilterCategory.Type::new);
    public static final DeferredHolder<ICategory.Type<?>, RecipeBookCategoryCategory.Type> RECIPE_BOOK_CATEGORY = REGISTER
        .register("recipe_book_category", RecipeBookCategoryCategory.Type::new);

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
