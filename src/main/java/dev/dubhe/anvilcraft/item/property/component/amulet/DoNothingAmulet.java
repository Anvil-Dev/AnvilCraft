package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record DoNothingAmulet() implements IAmulet {
    private static final DoNothingAmulet INSTANCE = new DoNothingAmulet();

    @Override
    public Type getType() {
        return ModAmuletTypes.DO_NOTHING.get();
    }

    public static class Type implements IAmulet.Type<DoNothingAmulet> {
        public static final MapCodec<DoNothingAmulet> CODEC = MapCodec.unit(DoNothingAmulet.INSTANCE);
        public static final StreamCodec<ByteBuf, DoNothingAmulet> STREAM_CODEC = StreamCodec.unit(DoNothingAmulet.INSTANCE);

        @Override
        public MapCodec<DoNothingAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DoNothingAmulet> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
