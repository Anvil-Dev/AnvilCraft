package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.IntegrationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public class OpenIntegrationScreenPacket implements CustomPacketPayload {
    public static final Type<OpenIntegrationScreenPacket> TYPE = new Type<>(AnvilCraft.of("open_integration_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenIntegrationScreenPacket> STREAM_CODEC = StreamCodec.ofMember(
        OpenIntegrationScreenPacket::encode,
        OpenIntegrationScreenPacket::new
    );
    public static final IPayloadHandler<OpenIntegrationScreenPacket> HANDLER = OpenIntegrationScreenPacket::clientHandler;

    public OpenIntegrationScreenPacket() {
    }

    public OpenIntegrationScreenPacket(RegistryFriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public Type<OpenIntegrationScreenPacket> type() {
        return OpenIntegrationScreenPacket.TYPE;
    }

    public void clientHandler(IPayloadContext context) {
        context.enqueueWork(IntegrationUtil::openIntegrationScreen);
    }
}
