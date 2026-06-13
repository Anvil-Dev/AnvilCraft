package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.event.IonoCraftBackpackClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * 飘升机背包飞行状态同步包（Server → Client）。
 * 当玩家开始/停止用背包飞行时通知周边客户端，供粒子渲染精确判断。
 */
public record IonoCraftBackpackFlyingPacket(int playerId, boolean flying) implements IClientboundPacket {

    public static final Type<IonoCraftBackpackFlyingPacket> TYPE =
        IPacket.type(AnvilCraft.of("ionocraft_backpack_flying"));

    public static final StreamCodec<ByteBuf, IonoCraftBackpackFlyingPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        IonoCraftBackpackFlyingPacket::playerId,
        ByteBufCodecs.BOOL,
        IonoCraftBackpackFlyingPacket::flying,
        IonoCraftBackpackFlyingPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        IonoCraftBackpackClientHandler.onFlyingSync(this.playerId, this.flying);
    }
}
