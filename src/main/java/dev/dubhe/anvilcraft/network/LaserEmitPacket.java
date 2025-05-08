package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.util.NetworkUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class LaserEmitPacket implements CustomPacketPayload {
    public static final Type<LaserEmitPacket> TYPE = new Type<>(AnvilCraft.of("laser_emit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserEmitPacket> STREAM_CODEC =
        StreamCodec.ofMember(LaserEmitPacket::encode, LaserEmitPacket::new);
    public static final IPayloadHandler<LaserEmitPacket> HANDLER = LaserEmitPacket::clientHandler;

    private final int laserLevel;
    private final BlockPos laserBlockPos;
    private final BlockPos irradiateBlockPos;

    /**
     * 激光照射网络包
     */
    public LaserEmitPacket(int laserLevel, BlockPos laserBlockPos, BlockPos irradiateBlockPos) {
        this.laserLevel = laserLevel;
        this.laserBlockPos = laserBlockPos;
        this.irradiateBlockPos = irradiateBlockPos;
    }

    /**
     * 激光照射网络包
     */
    public LaserEmitPacket(RegistryFriendlyByteBuf buf) {
        this.laserLevel = buf.readVarInt();
        this.laserBlockPos = NetworkUtil.readVarIntBlockPos(buf);
        this.irradiateBlockPos = buf.readOptional(NetworkUtil::readVarIntBlockPos)
            .orElse(null);
    }

    /**
     *
     */
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(laserLevel);
        NetworkUtil.writeVarIntBlockPos(buf, laserBlockPos);
        buf.writeOptional(
            Optional.ofNullable(irradiateBlockPos),
            NetworkUtil::writeVarIntBlockPos
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     *
     */
    public static void clientHandler(LaserEmitPacket data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(data.laserBlockPos)
                instanceof BaseLaserBlockEntity baseLaserBlockEntity) {
                baseLaserBlockEntity.clientUpdate(
                    data.irradiateBlockPos,
                    data.laserLevel
                );
                Minecraft.getInstance().levelRenderer.setBlockDirty(
                    data.laserBlockPos,
                    false
                );
            }
        });
    }
}
