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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class HeliostatsIrradiationPack implements Packet {
    private final BlockPos blockPos;
    private final BlockPos irritatePos;

    /**
     * 定日镜照射网络包
     */
    public HeliostatsIrradiationPack(
            BlockPos blockPos,
            BlockPos irritatePos
    ) {
        this.blockPos = blockPos;
        this.irritatePos = irritatePos;
    }

    /**
     * 定日镜照射网络包
     */
    public HeliostatsIrradiationPack(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        this.irritatePos = buf.readNullable(FriendlyByteBuf::readBlockPos);
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.HELIOSTATS_IRRADIATION;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeNullable(irritatePos, FriendlyByteBuf::writeBlockPos);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(blockPos)
                instanceof HeliostatsBlockEntity heliostatsBlockEntity) {
                heliostatsBlockEntity.setIrritatePos(irritatePos);
            }
        });
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            if (player.level().getBlockEntity(blockPos) instanceof HeliostatsBlockEntity heliostatsBlockEntity) {
                new HeliostatsIrradiationPack(blockPos, heliostatsBlockEntity.getIrritatePos()).send(player);
            }
        });
    }
}
