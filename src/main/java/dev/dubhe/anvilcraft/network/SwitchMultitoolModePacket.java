package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record SwitchMultitoolModePacket(InteractionHand hand, int mode) implements CustomPacketPayload {
    public static final Type<SwitchMultitoolModePacket> TYPE = new Type<>(AnvilCraft.of("switch_multitool_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchMultitoolModePacket> STREAM_CODEC =
        StreamCodec.ofMember((packet, buf) -> {
            buf.writeEnum(packet.hand);
            buf.writeVarInt(packet.mode);
        }, (buf) -> new SwitchMultitoolModePacket(buf.readEnum(InteractionHand.class), buf.readVarInt()));
    public static final IPayloadHandler<SwitchMultitoolModePacket> HANDLER = SwitchMultitoolModePacket::handler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handler(SwitchMultitoolModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> MultitoolItem.setMode(context.player(), packet.hand, packet.mode));
    }
}
