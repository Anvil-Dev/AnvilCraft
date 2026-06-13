package dev.dubhe.anvilcraft.saved.storage.category.client;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.player.LocalPlayer;
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
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

public record RecipeBookCategoryCategory(
    ItemStackTemplate icon,
    Component name,
    ResourceKey<RecipeBookCategory> key
) implements IClientCategory {
    public RecipeBookCategoryCategory(ItemLike icon, Identifier suffix, ResourceKey<RecipeBookCategory> key) {
        this(new ItemStackTemplate(icon.asItem()), ICategory.constructName(suffix), key);
    }

    @Override
    public boolean testClient(UnlimitedItemStack stack) {
        Optional<RecipeBookCategory> category = BuiltInRegistries.RECIPE_BOOK_CATEGORY.get(this.key).map(Holder.Reference::value);
        if (category.isEmpty()) return false;
        List<RecipeCollection> collections = Optional.ofNullable(Minecraft.getInstance().player)
            .map(LocalPlayer::getRecipeBook)
            .map(book -> book.getCollection(category.get()))
            .orElse(List.of());
        for (RecipeCollection collection : collections) {
            for (RecipeDisplayEntry recipe : collection.getRecipes()) {
                for (ItemStack result : recipe.resultItems(ContextMap.EMPTY)) {
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
