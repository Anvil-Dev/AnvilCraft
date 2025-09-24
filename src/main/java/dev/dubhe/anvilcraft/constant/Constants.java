package dev.dubhe.anvilcraft.constant;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public abstract class Constants {
    public static final int TRANSMITTER_LINE_COLOR = 0x9966CCFF;

    public static final StreamCodec<ByteBuf, CustomPacketPayload.Type<?>> PAYLOAD_TYPE_STREAM_CODEC = ResourceLocation.STREAM_CODEC
        .map(CustomPacketPayload.Type::new, CustomPacketPayload.Type::id);

    private Constants() {
    }
}
