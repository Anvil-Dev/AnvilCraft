package dev.dubhe.anvilcraft.api.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface Packet {
    ResourceLocation getType();

    void encode(@NotNull FriendlyByteBuf buf);

    default void handler(@NotNull MinecraftServer server, ServerPlayer player) {
    }

    default void send(ServerPlayer player) {
        Network.sendPacket(player, this);
    }

    default void broadcastPacketAll() {
        Network.broadcastPacketAll(this);
    }

    default void broadcastTrackingChunk(LevelChunk chunk) {
        Network.broadcastPacketTrackingChunk(chunk, this);
    }

    @Environment(EnvType.CLIENT)
    default void handler() {
    }

    @Environment(EnvType.CLIENT)
    default void send() {
        Network.sendPacket(this);
    }
}
