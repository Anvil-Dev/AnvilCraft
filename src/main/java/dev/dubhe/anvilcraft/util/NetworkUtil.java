package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NetworkUtil {
    public static void writeVarIntBlockPos(FriendlyByteBuf buf, BlockPos pos) {
        buf.writeVarInt(pos.getX());
        buf.writeVarInt(pos.getY());
        buf.writeVarInt(pos.getZ());
    }

    public static BlockPos readVarIntBlockPos(FriendlyByteBuf buf) {
        return new BlockPos(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()
        );
    }

    public static void sendToAllPlayersExcluded(
        ServerLevel level,
        @Nullable ServerPlayer excluded,
        CustomPacketPayload payload,
        CustomPacketPayload... payloads
    ) {
        Packet<?> packet = makeClientboundPacket(payload, payloads);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.equals(excluded)) continue;
            player.connection.send(packet);
        }
    }

    /**
     * copied from {@link PacketDistributor}
     */
    private static Packet<?> makeClientboundPacket(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        if (payloads.length > 0) {
            final List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            packets.add(new ClientboundCustomPayloadPacket(payload));
            for (CustomPacketPayload otherPayload : payloads) {
                packets.add(new ClientboundCustomPayloadPacket(otherPayload));
            }
            return new ClientboundBundlePacket(packets);
        } else {
            return new ClientboundCustomPayloadPacket(payload);
        }
    }
}
