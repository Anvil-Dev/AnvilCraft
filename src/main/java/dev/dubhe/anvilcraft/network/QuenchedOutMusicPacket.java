package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.event.QuenchedOutMusicHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * 淬灭序曲音乐包（Server → Client）。
 *
 * <p>
 * 服务端在演化将进入超新星前预定时刻通知客户端开始播放 {@code quenched_out} 曲目；
 * 在演化被中断（锻星砧/增幅器被破坏、加速器被清除）时通知客户端立即停止。
 * </p>
 */
public record QuenchedOutMusicPacket(BlockPos pos, boolean start) implements IClientboundPacket {

    public static final Type<QuenchedOutMusicPacket> TYPE = IPacket.type(AnvilCraft.of("quenched_out_music"));

    public static final StreamCodec<ByteBuf, QuenchedOutMusicPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        QuenchedOutMusicPacket::pos,
        ByteBufCodecs.BOOL,
        QuenchedOutMusicPacket::start,
        QuenchedOutMusicPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (start) {
            QuenchedOutMusicHandler.start(pos);
        } else {
            QuenchedOutMusicHandler.stop(pos);
        }
    }
}