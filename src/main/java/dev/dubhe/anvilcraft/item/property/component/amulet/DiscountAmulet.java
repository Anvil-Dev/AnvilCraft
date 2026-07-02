package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DiscountAmulet(float rate) implements IAmulet {
    @Override
    public Type getType() {
        return ModAmuletTypes.DISCOUNT.get();
    }

    public static class Type implements IAmulet.Type<DiscountAmulet> {
        public static final MapCodec<DiscountAmulet> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT
                .fieldOf("rate")
                .forGetter(DiscountAmulet::rate)
        ).apply(inst, DiscountAmulet::new));
        public static final StreamCodec<ByteBuf, DiscountAmulet> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            DiscountAmulet::rate,
            DiscountAmulet::new
        );

        @Override
        public MapCodec<DiscountAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DiscountAmulet> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
