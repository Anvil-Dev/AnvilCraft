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
    public void init(Class<T> type) {
        throw new AssertionError();
    }

    /**
     * 从服务端往指定玩家发送网络包
     *
     * @param player 要发向的玩家
     * @param data   数据
     */
    public void send(ServerPlayer player, T data) {
        throw new AssertionError();
    }

    /**
     * 从客户端往服务端发送网络包
     *
     * @param data 数据
     */
    @Environment(EnvType.CLIENT)
    public void send(T data) {
        throw new AssertionError();
    }

    /**
     * 向所有玩家广播网络包
     *
     * @param data 数据
     */
    public void broadcastAll(T data) {
        throw new AssertionError();
    }

    /**
     * 向持有该区块的玩家广播网络包
     *
     * @param chunk 区块
     * @param data  数据
     */
    public void broadcastTrackingChunk(LevelChunk chunk, T data) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Packet> @NotNull Network<T> create(
        ResourceLocation type, Class<T> clazz, Function<FriendlyByteBuf, T> decoder
    ) {
        throw new AssertionError();
    }

    /**
     * 注册
     *
     * @param type    类型
     * @param clazz   类
     * @param decoder 解码器
     * @param <T>     包
     * @return 网络类型
     */
    @SuppressWarnings("UnreachableCode")
    public static <T extends Packet> ResourceLocation register(
        ResourceLocation type, Class<T> clazz, Function<FriendlyByteBuf, T> decoder
    ) {
        Network<T> network = Network.create(type, clazz, decoder);
        Network.NETWORK_MAP.put(type, network);
        network.init(clazz);
        return type;
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Packet> void sendPacket(ServerPlayer player, @NotNull T data) {
        Network<?> network = Network.NETWORK_MAP.get(data.getType());
        if (network != null) {
            ((Network<T>) network).send(player, data);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Packet> void sendPacket(@NotNull T data) {
        Network<?> network = Network.NETWORK_MAP.get(data.getType());
        if (network != null) {
            ((Network<T>) network).send(data);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Packet> void broadcastPacketAll(@NotNull T data) {
        Network<?> network = Network.NETWORK_MAP.get(data.getType());
        if (network != null) {
            ((Network<T>) network).broadcastAll(data);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Packet> void broadcastPacketTrackingChunk(LevelChunk chunk, @NotNull T data) {
        Network<?> network = Network.NETWORK_MAP.get(data.getType());
        if (network != null) {
            ((Network<T>) network).broadcastTrackingChunk(chunk, data);
        }
    }
}
