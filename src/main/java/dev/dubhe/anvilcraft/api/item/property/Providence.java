package dev.dubhe.anvilcraft.api.item.property;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record Providence() {
    public static final Providence INSTANCE = new Providence();
    public static final Codec<Providence> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, Providence> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
