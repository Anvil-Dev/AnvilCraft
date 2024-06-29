package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.init.ModNetworks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class HeliostatsIrradiationPack implements Packet {
    private final BlockPos blockPos;
    private final Vector3f normalVector3f;
    private final Vector3f irritateVector3f;
    private final float irritateDistance;
    private final HeliostatsBlockEntity.WorkResult workResult;

    /**
     * 定日镜照射网络包
     */
    public HeliostatsIrradiationPack(
            BlockPos blockPos,
            Vector3f normalVector3f,
            Vector3f irritateVector3f,
            float irritateDistance,
            HeliostatsBlockEntity.WorkResult workResult
    ) {
        this.blockPos = blockPos;
        this.normalVector3f = normalVector3f;
        this.irritateVector3f = irritateVector3f;
        this.irritateDistance = irritateDistance;
        this.workResult = workResult;
    }

    /**
     * 定日镜照射网络包
     */
    public HeliostatsIrradiationPack(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        this.normalVector3f = buf.readVector3f();
        this.irritateVector3f = buf.readVector3f();
        this.irritateDistance = buf.readFloat();
        this.workResult = HeliostatsBlockEntity.WorkResult.valueOf(buf.readUtf());

    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.HELIOSTATS_IRRADIATION;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeVector3f(normalVector3f);
        buf.writeVector3f(irritateVector3f);
        buf.writeFloat(irritateDistance);
        buf.writeUtf(workResult.name());
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(blockPos)
                instanceof HeliostatsBlockEntity heliostatsBlockEntity) {
                heliostatsBlockEntity.setNormalVector3f(normalVector3f);
                heliostatsBlockEntity.setIrritateVector3f(irritateVector3f);
                heliostatsBlockEntity.setIrritateDistance(irritateDistance);
                heliostatsBlockEntity.setWorkResult(workResult);
            }
        });
    }
}
