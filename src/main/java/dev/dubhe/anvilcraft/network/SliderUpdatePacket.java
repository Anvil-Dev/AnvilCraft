package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import dev.dubhe.anvilcraft.util.Callback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import lombok.Getter;

@Getter
public class SliderUpdatePacket implements CustomPacketPayload {
    public static final Type<SliderUpdatePacket> TYPE = new Type<>(AnvilCraft.of("slider_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SliderUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, SliderUpdatePacket::getValue, SliderUpdatePacket::new);
    public static final IPayloadHandler<SliderUpdatePacket> HANDLER = SliderUpdatePacket::serverHandler;

    private final int value;

    public SliderUpdatePacket(int value) {
        this.value = value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(SliderUpdatePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof SliderMenu menu)) return;
            Callback<Integer> callback = menu.getCallback();
            if (callback != null) callback.onValueChange(data.value);
        });
    }
}
