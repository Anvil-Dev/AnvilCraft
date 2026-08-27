package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record FilterCategory(ItemStack icon, Component name, FilterContent filter) implements ICategory {
    private static final Component DEFAULT_NAME = Component.translatable("category.anvilcraft.filter");

    public static FilterCategory from(ItemStack filter) {
        filter = filter.copyWithCount(1);
        return new FilterCategory(
            filter.copy(),
            FilterCategory.findNameFromFilter(filter),
            Objects.requireNonNull(filter.get(ModComponents.FILTER_CONTENT), "Not valid filter content")
        );
    }

    private static Component findNameFromFilter(ItemStack filter) {
        return Objects.requireNonNullElse(filter.get(DataComponents.CUSTOM_NAME), FilterCategory.DEFAULT_NAME);
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return this.filter.filter(stack.getStack());
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.FILTER.get();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FilterCategory(ItemStack icon1, Component name1, FilterContent filter1))) return false;
        return ItemStack.isSameItemSameComponents(this.icon(), icon1)
               && Objects.equals(this.name(), name1)
               && Objects.equals(this.filter(), filter1);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.icon()) * 31 + Objects.hash(this.name(), this.filter());
    }

    public static class Type implements ICategory.Type<FilterCategory> {
        public static final MapCodec<FilterCategory> CODEC = CodecUtil.mapCodec(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(FilterCategory::icon),
            ICategory.NAME_CODEC
                .fieldOf("name")
                .forGetter(FilterCategory::name),
            FilterContent.CODEC
                .fieldOf("filter")
                .forGetter(FilterCategory::filter),
            FilterCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, FilterCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            FilterCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            FilterCategory::name,
            FilterContent.STREAM_CODEC,
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
