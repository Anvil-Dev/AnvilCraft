package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.FrostGrindstoneMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record FrostGrindstoneSyncPacket(int index, boolean select) implements CustomPacketPayload {
    public static final Type<FrostGrindstoneSyncPacket> TYPE = new Type<>(AnvilCraft.of("frost_grindstone_sync"));
    public static final StreamCodec<ByteBuf, FrostGrindstoneSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        FrostGrindstoneSyncPacket::index,
        ByteBufCodecs.BOOL,
        FrostGrindstoneSyncPacket::select,
        FrostGrindstoneSyncPacket::new
    );
    public static final IPayloadHandler<FrostGrindstoneSyncPacket> HANDLER = FrostGrindstoneSyncPacket::serverHandler;

    @Override
    public Type<FrostGrindstoneSyncPacket> type() {
        return TYPE;
    }

    private void serverHandler(IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        ctx.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof FrostGrindstoneMenu menu)) return;
            if (this.select) {
                menu.select(this.index);
            } else {
                menu.unselect(this.index);
            }
        });
    }
}
