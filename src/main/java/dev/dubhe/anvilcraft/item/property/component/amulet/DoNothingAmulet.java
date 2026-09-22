package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record DoNothingAmulet() implements IAmulet {
    /// <b>注意：本实例仅用于序列化，护符注册表中的护符均为独立实例。</b><br>
    /// <b>因此请勿将其作为护符值使用，否则会导致 {@link DoNothingAmulet#equals(Object)} 的行为异常</b>
    public static final DoNothingAmulet INSTANCE = new DoNothingAmulet();

    @Override
    public Type getType() {
        return ModAmuletTypes.DO_NOTHING.get();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
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
