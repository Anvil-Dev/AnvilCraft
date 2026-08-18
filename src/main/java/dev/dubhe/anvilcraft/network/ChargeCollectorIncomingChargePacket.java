package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record ChargeCollectorIncomingChargePacket(BlockPos srcPos, BlockPos dstPos, double count) implements IClientboundPacket {
    public static final Type<ChargeCollectorIncomingChargePacket> TYPE = IPacket.type(AnvilCraft.of("incoming_charge"));
    public static final StreamCodec<ByteBuf, ChargeCollectorIncomingChargePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ChargeCollectorIncomingChargePacket::srcPos,
        BlockPos.STREAM_CODEC,
        ChargeCollectorIncomingChargePacket::dstPos,
        ByteBufCodecs.DOUBLE,
        ChargeCollectorIncomingChargePacket::count,
        ChargeCollectorIncomingChargePacket::new
    );

    @Override
    public Type<ChargeCollectorIncomingChargePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(this.count > 0)) return;
        ClientLevel level = (ClientLevel) player.level();
        Vec3 srcPos = this.srcPos.getCenter();
        Vec3 dstPos = this.dstPos.getCenter();
        Vec3 offset = dstPos.subtract(srcPos);
        RandomSource random = level.getRandom();
        final double dRandom = Math.clamp(random.nextGaussian() + 1, 1, 1.5);
        level.addParticle(
            ParticleTypes.END_ROD,
            srcPos.x + Math.clamp(random.nextGaussian(), 0, 0.3),
            srcPos.y + Math.clamp(random.nextGaussian(), 0, 0.3),
            srcPos.z + Math.clamp(random.nextGaussian(), 0, 0.3),
            (offset.x / 20d) * dRandom,
            (offset.y / 20d) * dRandom,
            (offset.z / 20d) * dRandom
        );
    }
}
