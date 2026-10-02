package dev.dubhe.anvilcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public record DataComponentPredicate(DataComponentPatch patch, boolean isNegate) implements Predicate<DataComponentMap> {
    public static final Codec<DataComponentPredicate> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        DataComponentPatch.CODEC
            .optionalFieldOf("patch", DataComponentPatch.EMPTY)
            .forGetter(DataComponentPredicate::patch),
        Codec.BOOL
            .optionalFieldOf("isNegate", false)
            .forGetter(DataComponentPredicate::isNegate)
    ).apply(ins, DataComponentPredicate::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate> STREAM_CODEC = StreamCodec.composite(
        DataComponentPatch.STREAM_CODEC,
        DataComponentPredicate::patch,
        ByteBufCodecs.BOOL,
        DataComponentPredicate::isNegate,
        DataComponentPredicate::new
    );

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean test(DataComponentMap components) {
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : this.patch.entrySet()) {
            DataComponentType<?> type = entry.getKey();
            Object value = components.get(type);
            Optional<?> expect = entry.getValue();
            if (
                expect.isEmpty() && value != null
                || expect.isPresent() && !expect.get().equals(value)
            ) {
                return this.isNegate;
            }
        }
        return !this.isNegate;
    }

    @Override
    public Predicate<DataComponentMap> negate() {
        return new DataComponentPredicate(this.patch, !this.isNegate);
    }

    public static class Builder {
        private final DataComponentPatch.Builder patch = DataComponentPatch.builder();
        private boolean isNegate = false;

        public <T> Builder expect(DataComponentType<? super T> component, @Nullable T value) {
            if (value != null) {
                this.patch.set(component, value);
            } else {
                this.patch.remove(component);
            }
            return this;
        }

        public <T> Builder expectNull(DataComponentType<? super T> component) {
            this.patch.remove(component);
            return this;
        }

        public Builder negate() {
            this.isNegate = true;
            return this;
        }

        public DataComponentPredicate build() {
            return new DataComponentPredicate(this.patch.build(), this.isNegate);
        }
    }
}
