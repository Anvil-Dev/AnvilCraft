package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record DoNothingAmulet() implements IAmulet {
    /// <b>注意：仅应作为 {@link DataComponentHolder#getOrDefault(DataComponentType, Object)} 的默认值使用。</b><br>
    /// <b>存入物品时请使用 {@link DoNothingAmulet#DoNothingAmulet()} 创建新实例，否则会导致 {@link IAmulet#canActAs(IAmulet)} 失效</b>
    public static final DoNothingAmulet INSTANCE = new DoNothingAmulet();

    @Override
    public Type getType() {
        return ModAmuletTypes.DO_NOTHING.get();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DoNothingAmulet doNothingAmulet && this.canActAs(doNothingAmulet);
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
