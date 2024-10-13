package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.ModInspectionClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record InspectionStateChangedPacket(ResourceLocation id, boolean state) implements CustomPacketPayload {
    public static final Type<InspectionStateChangedPacket> TYPE = new Type<>(AnvilCraft.of("inspection_state"));

    public static final StreamCodec<ByteBuf, InspectionStateChangedPacket> STREAM_CODEC = StreamCodec.composite(
        net.minecraft.resources.ResourceLocation.STREAM_CODEC,
        InspectionStateChangedPacket::id,
        ByteBufCodecs.BOOL,
        InspectionStateChangedPacket::state,
        InspectionStateChangedPacket::new
    );


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void acceptClient(InspectionStateChangedPacket packet, IPayloadContext ctx) {
        ModInspectionClient.INSTANCE.changeStateClient(packet.id, packet.state);
    }
}
