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

public record ResonanceMiningEffectPacket(BlockPos pos, int durationTicks) implements IClientboundPacket {
    public static final Type<ResonanceMiningEffectPacket> TYPE =
        IPacket.type(AnvilCraft.of("resonance_mining_effect"));
    public static final StreamCodec<ByteBuf, ResonanceMiningEffectPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ResonanceMiningEffectPacket::pos,
        ByteBufCodecs.VAR_INT,
        ResonanceMiningEffectPacket::durationTicks,
        ResonanceMiningEffectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ResonanceMiningEffectPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (this.durationTicks > 0) {
            SeismicBounceManager.getInstance().startResonance(this.pos, this.durationTicks);
        } else {
            SeismicBounceManager.getInstance().stopResonance(this.pos);
        }
    }
}
