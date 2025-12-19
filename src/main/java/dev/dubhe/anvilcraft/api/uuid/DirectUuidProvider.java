package dev.dubhe.anvilcraft.api.uuid;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModUuidProviders;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.UUID;

public class DirectUuidProvider implements IUuidProvider {
    private final UUID id;

    public DirectUuidProvider(UUID id) {
        this.id = id;
    }

    @Override
    public UUID get() {
        return this.id;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Type getType() {
        return ModUuidProviders.DIRECT.get();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DirectUuidProvider that)) return false;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    public static class Type implements IUuidProvider.Type<DirectUuidProvider> {
        public static final MapCodec<DirectUuidProvider> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            UUIDUtil.CODEC
                .fieldOf("id")
                .forGetter(DirectUuidProvider::get)
        ).apply(ins, DirectUuidProvider::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, DirectUuidProvider> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            DirectUuidProvider::get,
            DirectUuidProvider::new
        );

        @Override
        public MapCodec<DirectUuidProvider> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DirectUuidProvider> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
