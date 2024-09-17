package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.jetbrains.annotations.NotNull;

public class HeliostatsIrradiationPacket implements CustomPacketPayload {
    public static final Type<HeliostatsIrradiationPacket> TYPE =
            new Type<>(AnvilCraft.of("heliostats_irradiation_pack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeliostatsIrradiationPacket> STREAM_CODEC =
            StreamCodec.ofMember(HeliostatsIrradiationPacket::encode, HeliostatsIrradiationPacket::new);
    public static final IPayloadHandler<HeliostatsIrradiationPacket> HANDLER = new DirectionalPayloadHandler<>(
            HeliostatsIrradiationPacket::clientHandler, HeliostatsIrradiationPacket::serverHandler);

    private final BlockPos blockPos;
    private final BlockPos irritatePos;

    /**
     * 定日镜照射网络包
     */
    public HeliostatsIrradiationPacket(BlockPos blockPos, BlockPos irritatePos) {
        this.blockPos = blockPos;
        this.irritatePos = irritatePos;
    }

    /**
     * 定日镜照射网络包
     */
    public HeliostatsIrradiationPacket(RegistryFriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        this.irritatePos = buf.readNullable(RegistryFriendlyByteBuf::readBlockPos);
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeNullable(irritatePos, RegistryFriendlyByteBuf::writeBlockPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     *
     */
    public static void clientHandler(HeliostatsIrradiationPacket data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getBlockEntity(data.blockPos)
                            instanceof HeliostatsBlockEntity heliostatsBlockEntity) {
                heliostatsBlockEntity.setIrritatePos(data.irritatePos);
            }
        });
    }

    /**
     *
     */
    public static void serverHandler(HeliostatsIrradiationPacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (player.level().getBlockEntity(data.blockPos) instanceof HeliostatsBlockEntity heliostatsBlockEntity) {
                var pack = new HeliostatsIrradiationPacket(data.blockPos, heliostatsBlockEntity.getIrritatePos());
                PacketDistributor.sendToPlayer(player, pack);
            }
        });
    }
}
