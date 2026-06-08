package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;

public record HasComponentCategory(
    ItemStackTemplate icon,
    Component name,
    DataComponentPredicate.Single<?> predicate
) implements ICategory {
    public <T extends DataComponentPredicate> HasComponentCategory(
        ItemLike icon,
        Identifier suffix,
        DataComponentPredicate.Type<T> type,
        T predicate
    ) {
        this(new ItemStackTemplate(icon.asItem()), ICategory.constructName(suffix), new DataComponentPredicate.Single<>(type, predicate));
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return this.predicate.predicate().matches(stack);
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.HAS_COMPONENT.get();
    }

    public static class Type implements ICategory.Type<HasComponentCategory> {
        public static final MapCodec<HasComponentCategory> CODEC = CodecUtil.mapCodec(
            ItemStackTemplate.CODEC
                .fieldOf("icon")
                .forGetter(HasComponentCategory::icon),
            ComponentSerialization.flatRestrictedCodec(Integer.MAX_VALUE)
                .fieldOf("name")
                .forGetter(HasComponentCategory::name),
            DataComponentPredicate.singleCodec("predicate")
                .forGetter(HasComponentCategory::predicate),
            HasComponentCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, HasComponentCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            HasComponentCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            HasComponentCategory::name,
            DataComponentPredicate.SINGLE_STREAM_CODEC,
            HasComponentCategory::predicate,
            HasComponentCategory::new
        );

        @Override
        public MapCodec<HasComponentCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasComponentCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
