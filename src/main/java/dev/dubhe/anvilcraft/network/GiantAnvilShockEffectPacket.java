package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.SeismicBounceManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * 巨型铁砧震波效果包（Server → Client）。
 *
 *  <p>
 * 告诉客户端在指定位置发生了震波，客户端播放方块弹跳动画。
 * </p>
 */
public record GiantAnvilShockEffectPacket(BlockPos centerPos, int radius) implements IClientboundPacket {

    public static final Type<GiantAnvilShockEffectPacket> TYPE =
        IPacket.type(AnvilCraft.of("giant_anvil_shock_effect"));

    public static final StreamCodec<ByteBuf, GiantAnvilShockEffectPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        GiantAnvilShockEffectPacket::centerPos,
        ByteBufCodecs.VAR_INT,
        GiantAnvilShockEffectPacket::radius,
        GiantAnvilShockEffectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        // 绑定到撼地粒子配置开关
        if (!AnvilCraft.CLIENT_CONFIG.groundHeaveParticlesEnabled) return;
        SeismicBounceManager.getInstance().triggerShock(this.centerPos, this.radius);
    }
}
