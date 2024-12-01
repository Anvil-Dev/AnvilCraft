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

public class AddTeslaFilterPacket implements CustomPacketPayload {
    public static final Type<AddTeslaFilterPacket> TYPE = new Type<>(AnvilCraft.of("tesla_filter_add"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddTeslaFilterPacket> STREAM_CODEC =
            StreamCodec.ofMember(AddTeslaFilterPacket::encode, AddTeslaFilterPacket::new);
    public static final IPayloadHandler<AddTeslaFilterPacket> HANDLER = AddTeslaFilterPacket::serverHandler;

    private final String id;
    private final String arg;

    public AddTeslaFilterPacket(String id, String arg) {
        this.id = id;
        this.arg = arg;
    }

    public AddTeslaFilterPacket(RegistryFriendlyByteBuf buf) {
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

    /**
     *
     */
    public static void serverHandler(AddTeslaFilterPacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (player.containerMenu instanceof TeslaTowerMenu menu) {
                menu.addFilter(data.id, data.arg);
            }
        });
    }
}
