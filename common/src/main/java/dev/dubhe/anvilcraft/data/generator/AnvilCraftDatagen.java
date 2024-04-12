package dev.dubhe.anvilcraft.data.generator;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.data.generator.lang.LangHandler;
import dev.dubhe.anvilcraft.data.generator.recipe.RecipesHandler;
import dev.dubhe.anvilcraft.data.generator.tags.TagsHandler;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class AnvilCraftDatagen {
    /**
     * 初始化生成器
     */
    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, TagsHandler::initItem);
        REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, TagsHandler::initBlock);
        REGISTRATE.addDataGenerator(ProviderType.LANG, LangHandler::init);
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, RecipesHandler::init);
    }

    public static @NotNull InventoryChangeTrigger.TriggerInstance has(ItemLike itemLike) {
        return RegistrateRecipeProvider.has(itemLike);
    }

    public static @NotNull InventoryChangeTrigger.TriggerInstance has(TagKey<Item> tag) {
        return RegistrateRecipeProvider.has(tag);
    }

    public static @NotNull String hasItem(@NotNull Item item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
