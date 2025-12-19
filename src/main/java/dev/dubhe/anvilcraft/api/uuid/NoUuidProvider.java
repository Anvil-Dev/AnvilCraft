package dev.dubhe.anvilcraft.api.uuid;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.ModUuidProviders;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class NoUuidProvider implements IUuidProvider {
    public NoUuidProvider() {
    }

    @Override
    public UUID get() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean canGet() {
        return false;
    }

    @Override
    public Type getType() {
        return ModUuidProviders.NO.get();
    }

    @Override
    public String toString() {
        return "No UUID";
    }

    public static class Type implements IUuidProvider.Type<NoUuidProvider> {
        public static final MapCodec<NoUuidProvider> CODEC = MapCodec.unit(NoUuidProvider::new);
        public static final StreamCodec<ByteBuf, NoUuidProvider> STREAM_CODEC = StreamCodec.unit(new NoUuidProvider());

        @Override
        public MapCodec<NoUuidProvider> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NoUuidProvider> streamCodec() {
            return STREAM_CODEC.cast();
        }
    }
}
