package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public class RemoveTeslaFilterPacket implements CustomPacketPayload {
    public static final Type<RemoveTeslaFilterPacket> TYPE = new Type<>(AnvilCraft.of("tesla_filter_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveTeslaFilterPacket> STREAM_CODEC =
            StreamCodec.ofMember(RemoveTeslaFilterPacket::encode, RemoveTeslaFilterPacket::new);
    public static final IPayloadHandler<RemoveTeslaFilterPacket> HANDLER = RemoveTeslaFilterPacket::serverHandler;

    private final String id;
    private final String arg;

    public RemoveTeslaFilterPacket(String id, String arg) {
        this.id = id;
        this.arg = arg;
    }

    public RemoveTeslaFilterPacket(RegistryFriendlyByteBuf buf) {
        this.id = buf.readUtf();
        this.arg = buf.readUtf();
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeUtf(arg);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(RemoveTeslaFilterPacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (player.containerMenu instanceof TeslaTowerMenu menu) {
                menu.removeFilter(data.id, data.arg);
            }
        });
    }
}
