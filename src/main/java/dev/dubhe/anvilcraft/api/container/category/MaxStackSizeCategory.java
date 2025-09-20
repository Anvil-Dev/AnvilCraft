package dev.dubhe.anvilcraft.api.container.category;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.init.ModCategories;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record MaxStackSizeCategory(ItemStack icon, Component name, MinMaxBounds.Ints maxSize) implements ICategory {
    public MaxStackSizeCategory(ItemLike icon, String prefix, MinMaxBounds.Ints maxSize) {
        this(icon.asItem().getDefaultInstance(), ICategory.constructName(prefix), maxSize);
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.maxSize.matches(stack.getMaxStackSize());
    }

    @Override
    public Type getType() {
        return ModCategories.MAX_STACK_SIZE.get();
    }

    public static class Type implements ICategory.Type<MaxStackSizeCategory> {
        public static final MapCodec<MaxStackSizeCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(MaxStackSizeCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(MaxStackSizeCategory::name),
            MinMaxBounds.Ints.CODEC
                .fieldOf("max_size")
                .forGetter(MaxStackSizeCategory::maxSize)
        ).apply(ins, MaxStackSizeCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, MaxStackSizeCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            MaxStackSizeCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            MaxStackSizeCategory::name,
            CodecUtil.codec2Stream(MinMaxBounds.Ints.CODEC),
            MaxStackSizeCategory::maxSize,
            MaxStackSizeCategory::new
        );

        @Override
        public MapCodec<MaxStackSizeCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MaxStackSizeCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
