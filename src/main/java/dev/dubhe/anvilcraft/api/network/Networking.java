package dev.dubhe.anvilcraft.api.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class Networking<C> {
    public static final Networking<ServerPlayNetworking.PlayChannelHandler> SERVER = new Networking<>() {
        public <T extends Packet> @NotNull PacketType<T> register(ResourceLocation location, Class<T> packet, Function<FriendlyByteBuf, T> deserializer) {
            this.handlerMap.put(location, (server, player, handler, buf, sender) -> this.receive(location, server, player, handler, buf, sender));
            return super.register(location, packet, deserializer);
        }

        @Override
        public void register() {
            for (Map.Entry<ResourceLocation, ServerPlayNetworking.PlayChannelHandler> entry : Networking.SERVER.handlerMap.entrySet()) {
                ServerPlayNetworking.registerGlobalReceiver(entry.getKey(), entry.getValue());
            }
        }
    };

    public static final Networking<ClientPlayNetworking.PlayChannelHandler> CLIENT = new Networking<>() {
        public <T extends Packet> @NotNull PacketType<T> register(ResourceLocation location, Class<T> packet, Function<FriendlyByteBuf, T> deserializer) {
            this.handlerMap.put(location, (server, handler, buf, sender) -> this.receive(location, server, handler, buf, sender));
            return super.register(location, packet, deserializer);
        }

        @Override
        public void register() {
            for (Map.Entry<ResourceLocation, ClientPlayNetworking.PlayChannelHandler> entry : Networking.CLIENT.handlerMap.entrySet()) {
                ClientPlayNetworking.registerGlobalReceiver(entry.getKey(), entry.getValue());
            }
        }
    };
    protected final Map<ResourceLocation, C> handlerMap = new HashMap<>();
    protected final Map<ResourceLocation, PacketHandler<? extends Packet>> networkMap = new HashMap<>();

    private Networking() {
    }

    public <T extends Packet> @NotNull PacketType<T> register(ResourceLocation location, Class<T> packet, Function<FriendlyByteBuf, T> deserializer) {
        this.networkMap.put(location, new PacketHandler<>(packet, deserializer));
        return PacketType.create(location, deserializer);
    }

    public <T extends Packet> @NotNull PacketType<T> register(String location, Class<T> packet, Function<FriendlyByteBuf, T> deserializer) {
        return this.register(AnvilCraft.of(location), packet, deserializer);
    }

    protected void receive(ResourceLocation location, @NotNull MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender sender) {
        PacketHandler<? extends Packet> handler1 = Networking.SERVER.networkMap.get(location);
        Packet packet = handler1.deserializer.apply(buf);
        packet.receive(server, player, handler, sender);
    }

    @Environment(EnvType.CLIENT)
    protected void receive(ResourceLocation location, @NotNull Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender sender) {
        PacketHandler<? extends Packet> handler1 = Networking.SERVER.networkMap.get(location);
        Packet packet = handler1.deserializer.apply(buf);
        packet.receive(client, handler, sender);
    }

    public abstract void register();

    protected record PacketHandler<T extends Packet>(Class<T> packet, Function<FriendlyByteBuf, T> deserializer) {
    }
}
