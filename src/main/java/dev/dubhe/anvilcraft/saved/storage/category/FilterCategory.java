package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import dev.dubhe.anvilcraft.item.utility.FilterItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Objects;

public record FilterCategory(ItemStackTemplate icon, Component name, ItemStack filter) implements ICategory {
    private static final Component DEFAULT_NAME = Component.translatable("category.anvilcraft.filter");

    public static FilterCategory from(ItemStack filter) {
        return new FilterCategory(ItemStackTemplate.fromNonEmptyStack(filter), FilterCategory.findNameFromFilter(filter), filter);
    }

    private static Component findNameFromFilter(ItemStack filter) {
        return Objects.requireNonNullElse(filter.getCustomName(), FilterCategory.DEFAULT_NAME);
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return FilterItem.filter(this.filter, stack.getStack());
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.FILTER.get();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FilterCategory(ItemStackTemplate icon1, Component name1, ItemStack filter1))) return false;
        return Objects.equals(this.name(), name1)
               && ItemStack.isSameItemSameComponents(this.filter(), filter1)
               && Objects.equals(this.icon(), icon1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.icon(), this.name()) * 31 + ItemStack.hashItemAndComponents(this.filter());
    }

    public static class Type implements ICategory.Type<FilterCategory> {
        public static final MapCodec<FilterCategory> CODEC = CodecUtil.mapCodec(
            ItemStackTemplate.CODEC
                .fieldOf("icon")
                .forGetter(FilterCategory::icon),
            ComponentSerialization.flatRestrictedCodec(Integer.MAX_VALUE)
                .fieldOf("name")
                .forGetter(FilterCategory::name),
            ItemStack.OPTIONAL_CODEC
                .fieldOf("filter")
                .forGetter(FilterCategory::filter),
            FilterCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, FilterCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            FilterCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            FilterCategory::name,
            ItemStack.OPTIONAL_STREAM_CODEC,
            FilterCategory::filter,
            FilterCategory::new
        );

        @Override
        public MapCodec<FilterCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FilterCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
