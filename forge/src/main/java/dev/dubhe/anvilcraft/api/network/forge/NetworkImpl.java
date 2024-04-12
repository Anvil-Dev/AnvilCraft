package dev.dubhe.anvilcraft.api.network.forge;

import dev.dubhe.anvilcraft.api.network.Network;
import dev.dubhe.anvilcraft.api.network.Packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class NetworkImpl<T> extends Network<T> {
    private static final String PROTOCOL_VERSION = "1";
    public SimpleChannel instance = null;

    @Override
    public void init(Class<T> type) {
        this.instance = NetworkRegistry.newSimpleChannel(
            this.getType(),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );
        this.instance.registerMessage(0, type, this::encode, this::decode, (data, ctx) -> {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.handler(data));
                else this.handler(data, sender.server, sender);
            });
            ctx.get().setPacketHandled(true);
        });
    }

    @Override
    public void send(T data) {
        if (this.instance == null) return;
        this.instance.sendToServer(data);
    }

    @Override
    public void send(ServerPlayer player, T data) {
        if (this.instance == null) return;
        this.instance.send(PacketDistributor.PLAYER.with(() -> player), data);
    }

    @Override
    public void broadcastAll(T data) {
        if (this.instance == null) return;
        this.instance.send(PacketDistributor.ALL.noArg(), data);
    }

    @Override
    public void broadcastTrackingChunk(LevelChunk chunk, T data) {
        if (this.instance == null) return;
        this.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), data);
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
