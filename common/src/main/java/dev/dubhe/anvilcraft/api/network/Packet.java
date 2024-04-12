package dev.dubhe.anvilcraft.api.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

/**
 * 网络包
 */
@SuppressWarnings("unused")
public interface Packet {
    ResourceLocation getType();

    void encode(@NotNull FriendlyByteBuf buf);

    default void handler(@NotNull MinecraftServer server, ServerPlayer player) {
    }

    @Environment(EnvType.CLIENT)
    default void handler() {
    }

    default void send(ServerPlayer player) {
        Network.sendPacket(player, this);
    }

    @Environment(EnvType.CLIENT)
    default void send() {
        Network.sendPacket(this);
    }

    default void broadcastPacketAll() {
        Network.broadcastPacketAll(this);
    }

    default void broadcastTrackingChunk(LevelChunk chunk) {
        Network.broadcastPacketTrackingChunk(chunk, this);
    }
}
