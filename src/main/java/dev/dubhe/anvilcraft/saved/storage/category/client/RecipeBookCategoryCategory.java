package dev.dubhe.anvilcraft.saved.storage.category.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RecipeBookCategoryCategory(
    ItemStack icon,
    Component name,
    String category
) implements IClientCategory {
    public RecipeBookCategoryCategory(ItemLike icon, ResourceLocation suffix, String category) {
        this(new ItemStack(icon.asItem()), ICategory.constructName(suffix), category);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean testClient(UnlimitedItemStack stack) {
        // 按序列化名匹配枚举（避免 valueOf 对不存在/大小写不符的名字抛异常）
        RecipeBookCategories found = null;
        for (RecipeBookCategories candidate : RecipeBookCategories.values()) {
            if (candidate.name().equalsIgnoreCase(this.category)) {
                found = candidate;
                break;
            }
        }
        final RecipeBookCategories categories = found;
        if (categories == null) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        List<RecipeCollection> collections = Optional.ofNullable(mc.player)
            .map(LocalPlayer::getRecipeBook)
            .map(book -> book.getCollection(categories))
            .orElse(List.of());
        for (RecipeCollection collection : collections) {
            for (RecipeHolder<?> recipe : collection.getRecipes()) {
                if (stack.isSameItemSameComponents(
                    recipe.value().getResultItem(Objects.requireNonNull(mc.getConnection()).registryAccess())
                )) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.RECIPE_BOOK_CATEGORY.get();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RecipeBookCategoryCategory(ItemStack icon1, Component name1, String category1))) return false;
        return ItemStack.isSameItemSameComponents(this.icon(), icon1)
               && Objects.equals(this.name(), name1)
               && Objects.equals(this.category(), category1);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.icon()) * 31 + Objects.hash(this.name(), this.category());
    }

    public static class Type implements ICategory.Type<RecipeBookCategoryCategory> {
        public static final MapCodec<RecipeBookCategoryCategory> CODEC = CodecUtil.mapCodec(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(RecipeBookCategoryCategory::icon),
            ComponentSerialization.CODEC
                .fieldOf("name")
                .forGetter(RecipeBookCategoryCategory::name),
            Codec.STRING
                .fieldOf("key")
                .forGetter(RecipeBookCategoryCategory::category),
            RecipeBookCategoryCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookCategoryCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            RecipeBookCategoryCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            RecipeBookCategoryCategory::name,
            ByteBufCodecs.STRING_UTF8,
            RecipeBookCategoryCategory::category,
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
