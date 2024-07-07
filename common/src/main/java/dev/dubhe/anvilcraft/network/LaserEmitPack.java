package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.init.ModNetworks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class LaserEmitPack implements Packet {
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
    public LaserEmitPack(FriendlyByteBuf buf) {
        this.lightPowerLevel = buf.readInt();
        this.laserBlockPos = buf.readBlockPos();
        if (buf.readBoolean()) {
            this.irradiateBlockPos = null;
            return;
        }
        this.irradiateBlockPos = buf.readBlockPos();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.LASER_EMIT;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(lightPowerLevel);
        buf.writeBlockPos(laserBlockPos);
        buf.writeBoolean(irradiateBlockPos == null);
        if (irradiateBlockPos != null)
            buf.writeBlockPos(irradiateBlockPos);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(laserBlockPos)
                instanceof BaseLaserBlockEntity baseLaserBlockEntity) {
                baseLaserBlockEntity.irradiateBlockPos = irradiateBlockPos;
                baseLaserBlockEntity.laserLevel = lightPowerLevel;
            }
        });
    }
}
