package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Objects;

/**
 * 依据服务端配方书分类（{@link CraftingBookCategory}）匹配物品的分类：
 * 遍历服务端配方表中该分类的合成配方，物品作为任一配方输出即视为属于该分类。
 * 服务端可求值，无需客户端配方书。
 */
public record CraftingBookCategoryCategory(
    ItemStack icon,
    Component name,
    CraftingBookCategory category
) implements ICategory {
    public CraftingBookCategoryCategory(ItemLike icon, ResourceLocation suffix, CraftingBookCategory category) {
        this(new ItemStack(icon.asItem()), ICategory.constructName(suffix), category);
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) {
                continue;
            }
            if (recipe.category() != this.category) {
                continue;
            }
            ItemStack result = recipe.getResultItem(server.registryAccess());
            if (stack.isSameItemSameComponents(result)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.CRAFTING_BOOK_CATEGORY.get();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CraftingBookCategoryCategory(ItemStack icon1, Component name1, CraftingBookCategory category1))) return false;
        return ItemStack.isSameItemSameComponents(this.icon(), icon1)
               && Objects.equals(this.name(), name1)
               && this.category == category1;
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.icon()) * 31 + Objects.hash(this.name(), this.category());
    }

    public static class Type implements ICategory.Type<CraftingBookCategoryCategory> {
        public static final MapCodec<CraftingBookCategoryCategory> CODEC = CodecUtil.mapCodec(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(CraftingBookCategoryCategory::icon),
            ICategory.NAME_CODEC
                .fieldOf("name")
                .forGetter(CraftingBookCategoryCategory::name),
            CraftingBookCategory.CODEC
                .fieldOf("category")
                .forGetter(CraftingBookCategoryCategory::category),
            CraftingBookCategoryCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CraftingBookCategoryCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            CraftingBookCategoryCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            CraftingBookCategoryCategory::name,
            CraftingBookCategory.STREAM_CODEC,
            CraftingBookCategoryCategory::category,
            CraftingBookCategoryCategory::new
        );

        @Override
        public MapCodec<CraftingBookCategoryCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CraftingBookCategoryCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
