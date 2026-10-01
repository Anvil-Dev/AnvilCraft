package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.AnvilParticleManager;
import dev.dubhe.anvilcraft.client.support.SeismicBounceManager;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 巨型铁砧震波效果包（Server → Client）。
 *
 *  <p>
 * 告诉客户端在指定位置发生了震波，客户端播放方块弹跳动画、撼地粒子与音效。
 * </p>
 */
public record GiantAnvilShockEffectPacket(BlockPos centerPos, int radius, boolean isResin, float pitch) implements IClientboundPacket {
    public static final Type<GiantAnvilShockEffectPacket> TYPE = IPacket.type(AnvilCraft.of("giant_anvil_shock_effect"));
    public static final StreamCodec<ByteBuf, GiantAnvilShockEffectPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        GiantAnvilShockEffectPacket::centerPos,
        ByteBufCodecs.VAR_INT,
        GiantAnvilShockEffectPacket::radius,
        ByteBufCodecs.BOOL,
        GiantAnvilShockEffectPacket::isResin,
        ByteBufCodecs.FLOAT,
        GiantAnvilShockEffectPacket::pitch,
        GiantAnvilShockEffectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return GiantAnvilShockEffectPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (AnvilCraft.CLIENT_CONFIG.effects.displayGiantAnvilShockBlockBounceAnimation) {
            SeismicBounceManager.getInstance().triggerShock(this.centerPos, this.radius);
        }
        if (AnvilCraft.CLIENT_CONFIG.effects.displayGiantAnvilShockParticles) {
            AnvilParticleManager.giantAnvilShock((ClientLevel) player.level(), this.centerPos, this.radius);
        }
        if (AnvilCraft.CLIENT_CONFIG.effects.playGiantAnvilShockSound) {
            BlockPos pos = this.centerPos.above(2);
            if (this.isResin) {
                player.level().playSound(
                    player,
                    pos,
                    ModSoundEvents.GIANT_ANVIL_RESIN_SHOCK.get(),
                    SoundSource.BLOCKS,
                    2.0f,
                    this.pitch
                );
            } else {
                player.level().playSound(
                    player,
                    pos,
                    ModSoundEvents.GIANT_ANVIL_SHOCK.get(),
                    SoundSource.BLOCKS,
                    1.8f,
                    this.pitch
                );
            }
        }
    }
}
