package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.SliderScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import lombok.Getter;

@Getter
public class SliderInitPacket implements CustomPacketPayload {
    public static final Type<SliderInitPacket> TYPE = new Type<>(AnvilCraft.of("slider_init"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SliderInitPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SliderInitPacket::getValue,
            ByteBufCodecs.INT,
            SliderInitPacket::getMin,
            ByteBufCodecs.INT,
            SliderInitPacket::getMax,
            SliderInitPacket::new);
    public static final IPayloadHandler<SliderInitPacket> HANDLER = SliderInitPacket::clientHandler;

    private final int value;
    private final int min;
    private final int max;

    /**
     * @param value 当前值
     * @param min 最小值
     * @param max 最大值
     */
    public SliderInitPacket(int value, int min, int max) {
        this.value = value;
        this.min = min;
        this.max = max;
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
