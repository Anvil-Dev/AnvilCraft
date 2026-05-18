package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record Providence() {
    public static final Providence INSTANCE = new Providence();
    public static final MapCodec<Providence> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, Providence> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
