package dev.dubhe.anvilcraft.api.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public interface Packet extends FabricPacket {
    default void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PacketSender responseSender) {
    }

    @Environment(EnvType.CLIENT)
    default void receive(Minecraft client, ClientPacketListener handler, PacketSender responseSender) {
    }

    default void send(ServerPlayer player) {
        ServerPlayNetworking.send(player, this);
    }

    @Environment(EnvType.CLIENT)
    default void send() {
        ClientPlayNetworking.send(this);
    }
}
