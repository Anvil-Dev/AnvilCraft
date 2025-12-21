package dev.dubhe.anvilcraft.api.sc.category.provider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.sc.category.ICategory;
import dev.dubhe.anvilcraft.init.ModRegistries;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter(AccessLevel.PROTECTED)
public class CategoryProvider implements Function<HolderLookup.RegistryLookup<ICategory>, ICategory> {
    public static final Codec<CategoryProvider> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Codec.xor(ResourceKey.codec(ModRegistries.CATEGORY_KEY), ICategory.CODEC)
            .fieldOf("value")
            .forGetter(CategoryProvider::getProvider)
    ).apply(ins, CategoryProvider::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryProvider> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.either(ResourceKey.streamCodec(ModRegistries.CATEGORY_KEY), ICategory.STREAM_CODEC),
        CategoryProvider::getProvider,
        CategoryProvider::new
    );
    private final Either<ResourceKey<ICategory>, ICategory> provider;
    private ICategory value;

    public CategoryProvider(ResourceKey<ICategory> key) {
        this(Either.left(key));
    }

    public CategoryProvider(ICategory category) {
        this(Either.right(category));

        this.value = category;
    }

    private CategoryProvider(Either<ResourceKey<ICategory>, ICategory> provider) {
        this.provider = provider;
        if (this.isCustom()) this.value = this.provider.right().orElseThrow();
    }

    public Optional<ICategory> get() {
        return Optional.ofNullable(this.value);
    }

    public ICategory get(Supplier<HolderLookup.RegistryLookup<ICategory>> lookupGetter) {
        return this.value == null ? this.apply(lookupGetter.get()) : this.value;
    }

    @Override
    public ICategory apply(HolderLookup.RegistryLookup<ICategory> lookup) {
        return this.value = this.provider.map(
            key -> lookup.getOrThrow(key).value(),
            Function.identity()
        );
    }

    public boolean isCustom() {
        return this.provider.right().isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CategoryProvider that)) return false;
        return Objects.equals(this.getProvider(), that.getProvider());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getProvider());
    }
}
