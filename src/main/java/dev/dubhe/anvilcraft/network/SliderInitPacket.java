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

@Getter
public class SliderInitPacket implements CustomPacketPayload {
    public static final Type<SliderInitPacket> TYPE = new Type<>(AnvilCraft.of("slider_init"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SliderInitPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        SliderInitPacket::getValue,
        SliderInitPacket::new);
    public static final IPayloadHandler<SliderInitPacket> HANDLER = SliderInitPacket::clientHandler;

    private final int value;

    public SliderInitPacket(int value) {
        this.value = value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(SliderInitPacket data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof SliderScreen screen)) return;
            screen.setValue(data.value);
        });
    }
}
