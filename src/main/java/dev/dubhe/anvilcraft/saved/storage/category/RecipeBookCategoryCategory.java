package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Optional;

public record RecipeBookCategoryCategory(
    ItemStackTemplate icon,
    Component name,
    ResourceKey<RecipeBookCategory> key
) implements ICategory {
    public RecipeBookCategoryCategory(ItemLike icon, Identifier suffix, ResourceKey<RecipeBookCategory> key) {
        this(new ItemStackTemplate(icon.asItem()), ICategory.constructName(suffix), key);
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        Optional<RecipeBookCategory> category = BuiltInRegistries.RECIPE_BOOK_CATEGORY.get(this.key).map(Holder.Reference::value);
        if (category.isEmpty()) {
            return false;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (!recipe.recipeBookCategory().equals(category.get())) {
                continue;
            }
            for (RecipeDisplay display : recipe.display()) {
                for (ItemStack result : display.result().resolveForStacks(ContextMap.EMPTY)) {
                    if (stack.isSameItemSameComponents(result)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.RECIPE_BOOK_CATEGORY.get();
    }

    public static class Type implements ICategory.Type<RecipeBookCategoryCategory> {
        public static final MapCodec<RecipeBookCategoryCategory> CODEC = CodecUtil.mapCodec(
            ItemStackTemplate.CODEC
                .fieldOf("icon")
                .forGetter(RecipeBookCategoryCategory::icon),
            ComponentSerialization.flatRestrictedCodec(Integer.MAX_VALUE)
                .fieldOf("name")
                .forGetter(RecipeBookCategoryCategory::name),
            ResourceKey.codec(Registries.RECIPE_BOOK_CATEGORY)
                .fieldOf("key")
                .forGetter(RecipeBookCategoryCategory::key),
            RecipeBookCategoryCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookCategoryCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            RecipeBookCategoryCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            RecipeBookCategoryCategory::name,
            ResourceKey.streamCodec(Registries.RECIPE_BOOK_CATEGORY),
            RecipeBookCategoryCategory::key,
            RecipeBookCategoryCategory::new
        );

        @Override
        public MapCodec<RecipeBookCategoryCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RecipeBookCategoryCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
