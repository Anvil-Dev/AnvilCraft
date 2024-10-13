package dev.dubhe.anvilcraft.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Random;

@MethodsReturnNonnullByDefault
public record ChargeCollectorIncomingChargePacket(
    BlockPos srcPos,
    BlockPos dstPos,
    double count
) implements CustomPacketPayload {
    private static final Random RANDOM = new Random(System.nanoTime());
    public static final Type<ChargeCollectorIncomingChargePacket> TYPE = new Type<>(AnvilCraft.of("incoming_charge"));

    public static final Codec<ChargeCollectorIncomingChargePacket> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        BlockPos.CODEC.fieldOf("srcPos").forGetter(o -> o.srcPos),
        BlockPos.CODEC.fieldOf("dstPos").forGetter(o -> o.dstPos),
        Codec.DOUBLE.fieldOf("count").forGetter(o -> o.count)
    ).apply(ins, ChargeCollectorIncomingChargePacket::new));

    public static final StreamCodec<? super RegistryFriendlyByteBuf, ChargeCollectorIncomingChargePacket> STREAM_CODEC =
        StreamCodec.composite(
            net.minecraft.core.BlockPos.STREAM_CODEC,
            ChargeCollectorIncomingChargePacket::srcPos,
            net.minecraft.core.BlockPos.STREAM_CODEC,
            ChargeCollectorIncomingChargePacket::dstPos,
            ByteBufCodecs.DOUBLE,
            ChargeCollectorIncomingChargePacket::count,
            ChargeCollectorIncomingChargePacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void acceptClient(ChargeCollectorIncomingChargePacket packet, IPayloadContext ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        Vec3 srcPos = packet.srcPos.getCenter();
        Vec3 dstPos = packet.dstPos.getCenter();
        Vec3 offset = dstPos.subtract(srcPos);
        RANDOM.setSeed(System.nanoTime());
        double dRandom = Math.clamp(RANDOM.nextGaussian() + 1, 1, 1.5);
        level.addParticle(
            ParticleTypes.END_ROD,
            srcPos.x + Math.clamp(RANDOM.nextGaussian(), 0, 0.3),
            srcPos.y + Math.clamp(RANDOM.nextGaussian(), 0, 0.3),
            srcPos.z + Math.clamp(RANDOM.nextGaussian(), 0, 0.3),
            (offset.x / 20d) * dRandom,
            (offset.y / 20d) * dRandom,
            (offset.z / 20d) * dRandom
        );
    }
}
