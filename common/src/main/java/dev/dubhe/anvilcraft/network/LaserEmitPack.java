package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.api.network.Packet;
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
    private final BlockPos laserBlockPos;
    private final BlockPos irradiateBlockPos;

    public LaserEmitPack(BlockPos laserBlockPos, BlockPos irradiateBlockPos) {
        this.laserBlockPos = laserBlockPos;
        this.irradiateBlockPos = irradiateBlockPos;
    }

    public LaserEmitPack(FriendlyByteBuf buf) {
        this.laserBlockPos = buf.readBlockPos();
        this.irradiateBlockPos = buf.readBlockPos();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.LASER_EMIT;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeBlockPos(laserBlockPos);
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
            }
        });
    }
}
