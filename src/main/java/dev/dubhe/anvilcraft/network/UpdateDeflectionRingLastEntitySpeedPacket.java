package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public class UpdateDeflectionRingLastEntitySpeedPacket implements CustomPacketPayload {
    public static final Type<UpdateDeflectionRingLastEntitySpeedPacket> TYPE = new Type<>(AnvilCraft.of("client_update_deflection_ring_last_entity_speed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateDeflectionRingLastEntitySpeedPacket> STREAM_CODEC =
            StreamCodec.ofMember(UpdateDeflectionRingLastEntitySpeedPacket::encode, UpdateDeflectionRingLastEntitySpeedPacket::new);
    public static final IPayloadHandler<UpdateDeflectionRingLastEntitySpeedPacket> HANDLER = UpdateDeflectionRingLastEntitySpeedPacket::clientHandler;

    private final BlockPos pos;
    private final double speed;

    public UpdateDeflectionRingLastEntitySpeedPacket(BlockPos pos, double speed) {
        this.pos = pos;
        this.speed = speed;
    }

    public UpdateDeflectionRingLastEntitySpeedPacket(@NotNull RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.speed = buf.readDouble();
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeDouble(speed);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(UpdateDeflectionRingLastEntitySpeedPacket data, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (mc.level == null) return;
            if (!(mc.level.getBlockEntity(data.pos) instanceof DeflectionRingBlockEntity deflectionRingBlockEntity))
                return;
            deflectionRingBlockEntity.setLastEntitySpeed(data.speed);
        });
    }
}
