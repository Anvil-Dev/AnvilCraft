package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public record AndCategory(ItemStackTemplate icon, Component name, List<ICategory> categories) implements ICategory {
    public AndCategory(ItemLike icon, Identifier suffix, ICategory... categories) {
        this(new ItemStackTemplate(icon.asItem()), ICategory.constructName(suffix), List.of(categories));
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        for (ICategory category : this.categories) {
            if (!category.test(stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.AND.get();
    }

    public static class Type implements ICategory.Type<AndCategory> {
        public static final MapCodec<AndCategory> CODEC = CodecUtil.mapCodec(
            ItemStackTemplate.CODEC
                .fieldOf("icon")
                .forGetter(AndCategory::icon),
            ComponentSerialization.flatRestrictedCodec(Integer.MAX_VALUE)
                .fieldOf("name")
                .forGetter(AndCategory::name),
            ICategory.CODEC
                .listOf()
                .fieldOf("categories")
                .forGetter(AndCategory::categories),
            AndCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, AndCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            AndCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            AndCategory::name,
            ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
            AndCategory::categories,
            AndCategory::new
        );

        @Override
        public MapCodec<AndCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AndCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
