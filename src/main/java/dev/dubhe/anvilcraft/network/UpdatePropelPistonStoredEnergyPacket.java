package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.PropelPistonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record UpdatePropelPistonStoredEnergyPacket(BlockPos pos, Integer energy) implements CustomPacketPayload {
    public static final Type<UpdatePropelPistonStoredEnergyPacket> TYPE = new Type<>(AnvilCraft.of(
        "client_update_propel_piston_stored_energy"
    ));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePropelPistonStoredEnergyPacket> STREAM_CODEC =
        StreamCodec.ofMember(UpdatePropelPistonStoredEnergyPacket::encode, UpdatePropelPistonStoredEnergyPacket::new);
    public static final IPayloadHandler<UpdatePropelPistonStoredEnergyPacket> HANDLER = UpdatePropelPistonStoredEnergyPacket::clientHandler;

    private void clientHandler(IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (mc.level == null) {
                return;
            }
            if (!(mc.level.getBlockEntity(pos) instanceof PropelPistonBlockEntity propelPistonBlockEntity)) {
                return;
            }
            propelPistonBlockEntity.updateStoredEnergy(energy);
        });
    }

    public UpdatePropelPistonStoredEnergyPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readInt());
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(energy);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
