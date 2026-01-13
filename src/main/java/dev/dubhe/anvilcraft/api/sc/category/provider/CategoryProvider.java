package dev.dubhe.anvilcraft.api.sc.category.provider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.sc.category.ICategory;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class CategoryProvider {
    public static final Codec<CategoryProvider> CODEC = Codec.lazyInitialized(() -> Codec.mapEither(Key.CODEC, Custom.CODEC).xmap(
        Either::unwrap,
        cp -> cp instanceof Key key ? Either.left(key) : Either.right(Util.cast(cp))
    ).codec());
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryProvider> STREAM_CODEC = StreamCodec.recursive(
        it -> ByteBufCodecs.either(Key.STREAM_CODEC, Custom.STREAM_CODEC)
            .map(Either::unwrap, cp -> cp instanceof Key key ? Either.left(key) : Either.right(Util.cast(cp)))
    );

    public static CategoryProvider create(ResourceKey<ICategory> key) {
        return new Key(key);
    }

    public static CategoryProvider create(ICategory value) {
        return new Custom(value);
    }

    public abstract Optional<ICategory> get();

    public abstract ICategory get(Supplier<HolderLookup.RegistryLookup<ICategory>> lookupGetter);

    public abstract ICategory get(HolderLookup.RegistryLookup<ICategory> lookup);

    public abstract boolean isCustom();

    @Getter
    static class Key extends CategoryProvider {
        public static final MapCodec<Key> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceKey.codec(ModRegistries.CATEGORY_KEY)
                .fieldOf("key")
                .forGetter(Key::getKey)
        ).apply(inst, Key::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(ModRegistries.CATEGORY_KEY),
            Key::getKey,
            Key::new
        );
        private final ResourceKey<ICategory> key;
        private ICategory value;

        public Key(ResourceKey<ICategory> key) {
            this.key = key;
        }

        @Override
        public Optional<ICategory> get() {
            return Optional.ofNullable(this.value);
        }

        @Override
        public ICategory get(Supplier<HolderLookup.RegistryLookup<ICategory>> lookupGetter) {
            return this.value == null ? this.get(lookupGetter.get()) : this.value;
        }

        @Override
        public ICategory get(HolderLookup.RegistryLookup<ICategory> lookup) {
            return this.value == null ? this.value = lookup.getOrThrow(this.key).value() : this.value;
        }

        @Override
        public boolean isCustom() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key key1)) return false;
            return Objects.equals(this.key, key1.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.key);
        }
    }

    @Getter
    static class Custom extends CategoryProvider {
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ICategory.CODEC
                .fieldOf("value")
                .forGetter(Custom::getValue)
        ).apply(inst, Custom::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Custom> STREAM_CODEC = StreamCodec.composite(
            ICategory.STREAM_CODEC,
            Custom::getValue,
            Custom::new
        );
        private final ICategory value;

        public Custom(ICategory value) {
            this.value = value;
        }

        @Override
        public Optional<ICategory> get() {
            return Optional.of(this.value);
        }

        @Override
        public ICategory get(Supplier<HolderLookup.RegistryLookup<ICategory>> lookupGetter) {
            return this.value;
        }

        @Override
        public ICategory get(HolderLookup.RegistryLookup<ICategory> lookup) {
            return this.value;
        }

        @Override
        public boolean isCustom() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Custom custom)) return false;
            return Objects.equals(this.value, custom.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }
    }
}
