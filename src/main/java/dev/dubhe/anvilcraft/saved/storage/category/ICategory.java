package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ICategory extends Predicate<UnlimitedItemStack> {
    Codec<ICategory> DIRECT_CODEC = Codec.lazyInitialized(
        () -> ModRegistries.CATEGORY_TYPE.byNameCodec().dispatch(ICategory::getType, Type::codec)
    );
    Codec<Holder<ICategory>> HOLDER_CODEC = RegistryFileCodec.create(ModRegistryKeys.CATEGORY, DIRECT_CODEC);
    Codec<ICategory> CODEC = Codec.of(
        new Encoder<>() {
            @Override
            public <T> DataResult<T> encode(ICategory input, DynamicOps<T> ops, T prefix) {
                if (!(ops instanceof RegistryOps<?> registryOps)) {
                    return DataResult.error(() -> "Can't access registry " + ModRegistryKeys.CATEGORY);
                }

                RegistryOps.RegistryInfo<ICategory> info = registryOps.lookupProvider.lookup(ModRegistryKeys.CATEGORY).orElseThrow();
                if (info.getter() instanceof HolderLookup.RegistryLookup<ICategory> lookup) {
                    Optional<Holder.Reference<ICategory>> ref = lookup.listElements()
                        .filter(innerRef -> input.equals(innerRef.value()))
                        .findFirst();
                    if (ref.isPresent()) {
                        return HOLDER_CODEC.encode(ref.get(), ops, prefix);
                    }
                }

                return HOLDER_CODEC.encode(
                    input instanceof HolderHolder(Holder<ICategory> category)
                    ? category
                    : Holder.direct(input),
                    ops,
                    prefix
                );
            }
        },
        HOLDER_CODEC.map(HolderHolder::new)
    );
    StreamCodec<RegistryFriendlyByteBuf, ICategory> STREAM_CODEC = ByteBufCodecs.registry(ModRegistryKeys.CATEGORY_TYPE)
        .dispatch(ICategory::getType, Type::streamCodec);

    ItemStackTemplate icon();

    Component name();

    @Override
    boolean test(UnlimitedItemStack stack);

    Type<? extends ICategory> getType();

    static Component constructName(Identifier suffix) {
        return Component.translatable("category." + suffix.toString().replace(':', '.'));
    }

    interface Type<C extends ICategory> extends ISerializer<C> {
    }

    record HolderHolder(Holder<ICategory> category) implements ICategory {
        @Override
        public ItemStackTemplate icon() {
            return this.category().value().icon();
        }

        @Override
        public Component name() {
            return this.category().value().name();
        }

        @Override
        public Type getType() {
            return ModCategoryTypes.WRAPPER.get();
        }

        @Override
        public boolean test(UnlimitedItemStack stack) {
            return this.category().value().test(stack);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof HolderHolder(Holder<ICategory> category1)) {
                return this.category().equals(category1);
            } else if (o instanceof ICategory category1) {
                return this.category().value().equals(category1);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.category().value());
        }

        public static class Type implements ICategory.Type<HolderHolder> {
            public static final MapCodec<HolderHolder> CODEC = CodecUtil.mapCodec(
                ICategory.HOLDER_CODEC
                    .fieldOf("category")
                    .forGetter(HolderHolder::category),
                HolderHolder::new
            );
            public static final StreamCodec<RegistryFriendlyByteBuf, HolderHolder> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.either(ByteBufCodecs.holderRegistry(ModRegistryKeys.CATEGORY), ICategory.STREAM_CODEC).map(
                    either -> either.map(Function.identity(), Holder::direct),
                    holder -> holder.kind() == Holder.Kind.DIRECT ? Either.right(holder.value()) : Either.left(holder)
                ),
                HolderHolder::category,
                HolderHolder::new
            );

            @Override
            public MapCodec<HolderHolder> codec() {
                return Type.CODEC;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, HolderHolder> streamCodec() {
                return Type.STREAM_CODEC;
            }
        }
    }
}
