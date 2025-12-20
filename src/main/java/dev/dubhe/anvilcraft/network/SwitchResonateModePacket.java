package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record SwitchResonateModePacket(InteractionHand hand, int mode) implements CustomPacketPayload {
    public static final Type<SwitchResonateModePacket> TYPE = new Type<>(AnvilCraft.of("switch_resonate_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchResonateModePacket> STREAM_CODEC =
        StreamCodec.ofMember(SwitchResonateModePacket::encode, SwitchResonateModePacket::decode);
    public static final IPayloadHandler<SwitchResonateModePacket> HANDLER = SwitchResonateModePacket::serverHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
        buf.writeVarInt(this.mode);
    }

    public static SwitchResonateModePacket decode(RegistryFriendlyByteBuf buf) {
        return new SwitchResonateModePacket(buf.readEnum(InteractionHand.class), buf.readVarInt());
    }

    public static void serverHandler(SwitchResonateModePacket data, IPayloadContext context) {
        context.enqueueWork(() -> ResonatorItem.set((ServerPlayer) context.player(), data.hand, data.mode));
    }
}
