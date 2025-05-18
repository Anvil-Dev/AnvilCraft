package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.SliderScreen;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record SliderInitPacket(int value, int min, int max) implements CustomPacketPayload {
    public static final Type<SliderInitPacket> TYPE = new Type<>(AnvilCraft.of("slider_init"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SliderInitPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        SliderInitPacket::value,
        ByteBufCodecs.INT,
        SliderInitPacket::min,
        ByteBufCodecs.INT,
        SliderInitPacket::max,
        SliderInitPacket::new
    );
    public static final IPayloadHandler<SliderInitPacket> HANDLER = SliderInitPacket::clientHandler;

    /**
     * @param value 当前值
     * @param min   最小值
     * @param max   最大值
     */
    public SliderInitPacket {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(SliderInitPacket data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof SliderScreen screen)) return;
            screen.setMin(data.min);
            screen.setMax(data.max);
            screen.setValue(data.value);
        });
    }
}
