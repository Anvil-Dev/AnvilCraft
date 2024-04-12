package dev.dubhe.anvilcraft.api.network.fabric;

import dev.dubhe.anvilcraft.api.network.Network;
import dev.dubhe.anvilcraft.api.network.Packet;
import dev.dubhe.anvilcraft.utils.fabric.ServerHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class NetworkImpl<T> extends Network<T> {
    @Override
    public void init(Class<T> type) {
        EnvType envType = FabricLoader.getInstance().getEnvironmentType();
        if (envType == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(this.getType(), (client, handler, buf, sender) ->
                this.handler(this.decode(buf)));
        }
        ServerPlayNetworking.registerGlobalReceiver(this.getType(), (server, player, handler, buf, sender) ->
            this.handler(this.decode(buf), server, player));
    }

    @Override
    public void send(T data) {
        FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
        this.encode(data, friendlyByteBuf);
        ClientPlayNetworking.send(this.getType(), friendlyByteBuf);
    }

    @Override
    public void send(ServerPlayer player, T data) {
        FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
        this.encode(data, friendlyByteBuf);
        ServerPlayNetworking.getSender(player).sendPacket(this.getType(), friendlyByteBuf);
    }

    @Override
    public void broadcastTrackingChunk(@NotNull LevelChunk chunk, T data) {
        FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
        this.encode(data, friendlyByteBuf);
        for (ServerPlayer player : ((ServerChunkCache) chunk.getLevel().getChunkSource())
            .chunkMap.getPlayers(chunk.getPos(), false)) {
            ServerPlayNetworking.getSender(player).sendPacket(this.getType(), friendlyByteBuf);
        }
    }

    @Override
    public void broadcastAll(T data) {
        FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
        this.encode(data, friendlyByteBuf);
        MinecraftServer server = ServerHooks.getServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.getSender(player).sendPacket(this.getType(), friendlyByteBuf);
        }
    }

    /**
     * 创建网络包类型
     *
     * @param type    类型
     * @param clazz   类
     * @param decoder 解码器
     * @param <M>     数据包
     * @return 网络包类型
     */
    public static <M extends Packet> @NotNull Network<M> create(
        ResourceLocation type, Class<M> clazz, Function<FriendlyByteBuf, M> decoder
    ) {
        return new NetworkImpl<>() {
            @Override
            public ResourceLocation getType() {
                return type;
            }

            @Override
            public void encode(@NotNull M data, @NotNull FriendlyByteBuf buf) {
                data.encode(buf);
            }

            @Override
            public M decode(@NotNull FriendlyByteBuf buf) {
                return decoder.apply(buf);
            }

            @Override
            public void handler(@NotNull M data) {
                data.handler();
            }

            @Override
            public void handler(@NotNull M data, MinecraftServer server, ServerPlayer player) {
                data.handler(server, player);
            }
        };
    }
}
