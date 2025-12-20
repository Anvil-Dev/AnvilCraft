package dev.dubhe.anvilcraft.api.container.category;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.sc.ModCategories;
import dev.dubhe.anvilcraft.item.FilterItem;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record FilterCategory(ItemStack icon, Component name, ItemStack filter) implements ICategory {
    public FilterCategory(ItemStack filter) {
        this(
            FilterContent.getFirstItem(filter),
            filter.getOrDefault(DataComponents.CUSTOM_NAME, ICategory.constructName("new")),
            new ItemStack(
                filter.getItemHolder(),
                1,
                DataComponentPatch.builder()
                    .set(ModComponents.FILTER_CONTENT, filter.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent()))
                    .build()
            )
        );
    }

    private FilterCategory(ItemStack icon, Component name, FilterContent content) {
        this(
            icon,
            name,
            new ItemStack(
                ModItems.FILTER,
                1,
                DataComponentPatch.builder()
                    .set(ModComponents.FILTER_CONTENT, content)
                    .build()
            )
        );
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return FilterItem.filter(this.filter, stack.getStack());
    }

    @Override
    public Type getType() {
        return ModCategories.FILTER.get();
    }

    private FilterContent asFilter() {
        return this.filter.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
    }

    public static class Type implements ICategory.Type<FilterCategory> {
        public static final MapCodec<FilterCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(FilterCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(FilterCategory::name),
            FilterContent.CODEC
                .fieldOf("filter")
                .forGetter(FilterCategory::asFilter)
        ).apply(ins, FilterCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, FilterCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            FilterCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            FilterCategory::name,
            FilterContent.STREAM_CODEC,
            FilterCategory::asFilter,
            FilterCategory::new
        );

        @Override
        public MapCodec<FilterCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FilterCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
