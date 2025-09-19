package dev.dubhe.anvilcraft.api.crate.category.client;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.api.crate.category.ICategory;
import dev.dubhe.anvilcraft.init.ModCategories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

public record RecipeBookCategoryCategory(ItemStack icon, Component name, String categories) implements IClientCategory {
    @OnlyIn(Dist.CLIENT)
    public static final Codec<RecipeBookCategories> CATEGORIES_CODEC = CodecUtil.enumCodecInLowerName(RecipeBookCategories.class);

    public RecipeBookCategoryCategory(ItemLike icon, String prefix, String categories) {
        this(icon.asItem().getDefaultInstance(), ICategory.constructName(prefix), categories);
    }

    @Override
    public boolean testClient(ItemStack stack) {
        Optional<RecipeBookCategories> categories = CATEGORIES_CODEC.decode(JavaOps.INSTANCE, this.categories)
            .result()
            .map(Pair::getFirst);
        if (categories.isEmpty()) return false;
        List<RecipeCollection> collections = Optional.ofNullable(Minecraft.getInstance().player)
            .map(LocalPlayer::getRecipeBook)
            .map(book -> book.getCollection(categories.get()))
            .orElse(List.of());
        for (RecipeCollection collection : collections) {
            for (RecipeHolder<?> recipe : collection.getRecipes()) {
                if (
                    ItemStack.isSameItemSameComponents(
                        recipe.value().getResultItem(Minecraft.getInstance().player.registryAccess()),
                        stack
                    )
                ) return true;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModCategories.RECIPE_BOOK_CATEGORY.get();
    }

    public static class Type implements ICategory.Type<RecipeBookCategoryCategory> {
        public static final MapCodec<RecipeBookCategoryCategory> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(RecipeBookCategoryCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(RecipeBookCategoryCategory::name),
            Codec.STRING
                .fieldOf("categories")
                .forGetter(RecipeBookCategoryCategory::categories)
        ).apply(inst, RecipeBookCategoryCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookCategoryCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            RecipeBookCategoryCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            RecipeBookCategoryCategory::name,
            ByteBufCodecs.STRING_UTF8,
            RecipeBookCategoryCategory::categories,
            RecipeBookCategoryCategory::new
        );

        @Override
        public MapCodec<RecipeBookCategoryCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RecipeBookCategoryCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
