package dev.dubhe.anvilcraft.api.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public abstract class Network<T> {
    public static final Map<ResourceLocation, Network<?>> NETWORK_MAP = new HashMap<>();

    /**
     * 获取网络包类型
     *
     * @return 网络包类型
     */
    public abstract ResourceLocation getType();

    /**
     * 服务端侧网络包处理器
     *
     * @param data   数据
     * @param server 服务端
     * @param player 发包的玩家
     */
    public void handler(T data, MinecraftServer server, ServerPlayer player) {
    }

    /**
     * 客户端侧网络包处理器
     *
     * @param data 数据
     */
    @Environment(EnvType.CLIENT)
    public void handler(T data) {
    }

    /**
     * 编码器
     *
     * @param data 数据
     * @param buf  数据包
     */
    public abstract void encode(T data, @NotNull FriendlyByteBuf buf);

    /**
     * 解码器
     *
     * @param buf 数据包
     * @return 解码出的对象
     */
    public abstract T decode(@NotNull FriendlyByteBuf buf);

    /**
     * 初始化
     */
    @ExpectPlatform
    public void init(Class<T> type) {
        throw new AssertionError();
    }

    /**
     * 从服务端往指定玩家发送网络包
     *
     * @param player 要发向的玩家
     * @param data   数据
     */
    @ExpectPlatform
    public void send(ServerPlayer player, T data) {
        throw new AssertionError();
    }

    /**
     * 向所有玩家广播网络包
     *
     * @param data 数据
     */
    @ExpectPlatform
    public void broadcastAll(T data) {
        throw new AssertionError();
    }

    /**
     * 向持有该区块的玩家广播网络包
     *
     * @param chunk 区块
     * @param data  数据
     */
    @ExpectPlatform
    public void broadcastTrackingChunk(LevelChunk chunk, T data) {
        throw new AssertionError();
    }

    /**
     * 从客户端往服务端发送网络包
     *
     * @param data 数据
     */
    @Environment(EnvType.CLIENT)
    @ExpectPlatform
    public void send(T data) {
        throw new AssertionError();
    }

    public static <MSG extends Packet> ResourceLocation register(ResourceLocation type, Class<MSG> clazz, Function<FriendlyByteBuf, MSG> decoder) {
        Network<MSG> network = new Network<>() {
            @Override
            public ResourceLocation getType() {
                return type;
            }

            @Override
            public void encode(@NotNull MSG data, @NotNull FriendlyByteBuf buf) {
                data.encode(buf);
            }

            @Override
            public MSG decode(@NotNull FriendlyByteBuf buf) {
                return decoder.apply(buf);
            }

            @Override
            public void handler(@NotNull MSG data) {
                data.handler();
            }

            @Override
            public void handler(@NotNull MSG data, MinecraftServer server, ServerPlayer player) {
                data.handler(server, player);
            }
        };
        Network.NETWORK_MAP.put(type, network);
        network.init(clazz);
        return type;
    }

    @SuppressWarnings("unchecked")
    protected static <MSG extends Packet> void sendPacket(ServerPlayer player, @NotNull MSG data) {
        Network<?> network = Network.NETWORK_MAP.get(data.getType());
        if (network != null) {
            ((Network<MSG>) network).send(player, data);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <MSG extends Packet> void sendPacket(@NotNull MSG data) {
        Network<?> network = Network.NETWORK_MAP.get(data.getType());
        if (network != null) {
            ((Network<MSG>) network).send(data);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <MSG extends Packet> void broadcastPacketAll(@NotNull MSG data) {
        Network<?> network = Network.NETWORK_MAP.get(data.getType());
        if (network != null) {
            ((Network<MSG>) network).broadcastAll(data);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <MSG extends Packet> void broadcastPacketTrackingChunk(LevelChunk chunk, @NotNull MSG data) {
        Network<?> network = Network.NETWORK_MAP.get(data.getType());
        if (network != null) {
            ((Network<MSG>) network).broadcastTrackingChunk(chunk, data);
        }
    }
}
