package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record RocketJumpPacket(double power) implements CustomPacketPayload {
    public static final Type<RocketJumpPacket> TYPE = new Type<>(AnvilCraft.of("rocket_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RocketJumpPacket> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.DOUBLE, RocketJumpPacket::power, RocketJumpPacket::new);
    public static final IPayloadHandler<RocketJumpPacket> HANDLER = RocketJumpPacket::clientHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(RocketJumpPacket data, IPayloadContext context) {
        LocalPlayer player = (LocalPlayer) context.player();
        context.enqueueWork(() -> player.setDeltaMovement(0, data.power, 0));
    }
}
