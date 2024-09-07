package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public class LaserEmitPack implements CustomPacketPayload {
    public static final Type<LaserEmitPack> TYPE = new Type<>(AnvilCraft.of("laser_emit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserEmitPack> STREAM_CODEC = StreamCodec.ofMember(
            LaserEmitPack::encode, LaserEmitPack::new
    );
    public static final IPayloadHandler<LaserEmitPack> HANDLER = LaserEmitPack::clientHandler;

    private final int lightPowerLevel;
    private final BlockPos laserBlockPos;
    private final BlockPos irradiateBlockPos;

    /**
     * 激光照射网络包
     */
    public LaserEmitPack(int lightPowerLevel, BlockPos laserBlockPos, BlockPos irradiateBlockPos) {
        this.lightPowerLevel = lightPowerLevel;
        this.laserBlockPos = laserBlockPos;
        this.irradiateBlockPos = irradiateBlockPos;
    }

    /**
     * 激光照射网络包
     */
    public LaserEmitPack(RegistryFriendlyByteBuf buf) {
        this.lightPowerLevel = buf.readInt();
        this.laserBlockPos = buf.readBlockPos();
        if (buf.readBoolean()) {
            this.irradiateBlockPos = null;
            return;
        }
        this.irradiateBlockPos = buf.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(lightPowerLevel);
        buf.writeBlockPos(laserBlockPos);
        buf.writeBoolean(irradiateBlockPos == null);
        if (irradiateBlockPos != null)
            buf.writeBlockPos(irradiateBlockPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(LaserEmitPack data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(data.laserBlockPos)
                instanceof BaseLaserBlockEntity baseLaserBlockEntity) {
                baseLaserBlockEntity.irradiateBlockPos = data.irradiateBlockPos;
                baseLaserBlockEntity.laserLevel = data.lightPowerLevel;
            }
        });
    }
}
