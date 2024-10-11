package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.init.ModNetworks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class ClientboundIncomingChargePacket implements Packet {
    private static final Random RANDOM = new Random(System.nanoTime());
    public final BlockPos srcPos;
    public final BlockPos dstPos;
    public final double count;

    /**
     * 收集电荷包
     */
    public ClientboundIncomingChargePacket(BlockPos srcPos, BlockPos dstPos, double count) {
        this.srcPos = srcPos;
        this.dstPos = dstPos;
        this.count = count;
    }

    public ClientboundIncomingChargePacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readBlockPos(), buf.readDouble());
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.CLIENT_INCOMING_CHARGE;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeBlockPos(srcPos);
        buf.writeBlockPos(dstPos);
        buf.writeDouble(count);
    }

    @Override
    public void handler() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Vec3 srcPos = this.srcPos.getCenter();
        Vec3 dstPos = this.dstPos.getCenter();
        Vec3 offset = dstPos.subtract(srcPos);
        RANDOM.setSeed(System.nanoTime());
        double rand = Mth.clamp(RANDOM.nextGaussian() + 1, 1, 1.5);
        level.addParticle(
            ParticleTypes.END_ROD,
            srcPos.x + Mth.clamp(RANDOM.nextGaussian(), 0, 0.3),
            srcPos.y + Mth.clamp(RANDOM.nextGaussian(), 0, 0.3),
            srcPos.z + Mth.clamp(RANDOM.nextGaussian(), 0, 0.3),
            (offset.x / 20d) * rand,
            (offset.y / 20d) * rand,
            (offset.z / 20d) * rand
        );
    }
}
