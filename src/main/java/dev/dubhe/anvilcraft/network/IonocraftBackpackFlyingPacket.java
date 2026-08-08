package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.event.IonocraftBackpackClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * 飘升机背包飞行状态同步包（Server → Client）。
 * 当玩家开始/停止用背包飞行时通知周边客户端，供粒子渲染精确判断。
 */
public record IonocraftBackpackFlyingPacket(int playerId, boolean flying) implements IClientboundPacket {

    public static final Type<IonocraftBackpackFlyingPacket> TYPE =
        IPacket.type(AnvilCraft.of("ionocraft_backpack_flying"));

    public static final StreamCodec<ByteBuf, IonocraftBackpackFlyingPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        IonocraftBackpackFlyingPacket::playerId,
        ByteBufCodecs.BOOL,
        IonocraftBackpackFlyingPacket::flying,
        IonocraftBackpackFlyingPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        IonocraftBackpackClientHandler.onFlyingSync(this.playerId, this.flying);
    }
}
