package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.api.container.ContainerStorages;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record ShulkerContainerScreenSyncOrderPacket(IntSet order, float scrollOffs) implements CustomPacketPayload {
    public static final Type<ShulkerContainerScreenSyncOrderPacket> TYPE = new Type<>(AnvilCraft.of(
        "shulker_container_screen_sync_order"
    ));
    public static final StreamCodec<FriendlyByteBuf, ShulkerContainerScreenSyncOrderPacket> STREAM_CODEC = StreamCodec.of(
        ShulkerContainerScreenSyncOrderPacket::writeTo,
        ShulkerContainerScreenSyncOrderPacket::readFrom
    );
    public static final IPayloadHandler<ShulkerContainerScreenSyncOrderPacket> HANDLER =
        ShulkerContainerScreenSyncOrderPacket::serverHandler;

    private static ShulkerContainerScreenSyncOrderPacket readFrom(FriendlyByteBuf buf) {
        IntSet order = new IntArraySet();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            order.add(buf.readVarInt());
        }
        return new ShulkerContainerScreenSyncOrderPacket(order, buf.readFloat());
    }

    private static void writeTo(FriendlyByteBuf buf, ShulkerContainerScreenSyncOrderPacket packet) {
        buf.writeVarInt(packet.order.size());
        for (int i : packet.order) {
            buf.writeVarInt(i);
        }

        buf.writeFloat(packet.scrollOffs);
    }

    @Override
    public Type<ShulkerContainerScreenSyncOrderPacket> type() {
        return TYPE;
    }

    private void serverHandler(IPayloadContext ctx) {
        if (!(ctx.player().containerMenu instanceof ShulkerContainerMenu menu)) return;
        ctx.enqueueWork(() -> menu.applyOrder(this.order, this.scrollOffs));
    }
}
