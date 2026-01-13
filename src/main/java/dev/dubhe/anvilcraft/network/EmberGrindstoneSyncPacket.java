package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.EmberGrindstoneMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record EmberGrindstoneSyncPacket(int index) implements CustomPacketPayload {
    public static final Type<EmberGrindstoneSyncPacket> TYPE = new Type<>(AnvilCraft.of("ember_grindstone_sync"));
    public static final StreamCodec<ByteBuf, EmberGrindstoneSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        EmberGrindstoneSyncPacket::index,
        EmberGrindstoneSyncPacket::new
    );
    public static final IPayloadHandler<EmberGrindstoneSyncPacket> HANDLER = EmberGrindstoneSyncPacket::serverHandler;

    @Override
    public Type<EmberGrindstoneSyncPacket> type() {
        return TYPE;
    }

    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof EmberGrindstoneMenu menu)) return;
            menu.setSelectedEnchantment(this.index);
        });
    }
}
