package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.PropelPistonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public record UpdatePropelPistonStoredEnergyPacket(BlockPos pos, int energy) implements IClientboundPacket {
    public static final Type<UpdatePropelPistonStoredEnergyPacket> TYPE = new Type<>(AnvilCraft.of(
        "client_update_propel_piston_stored_energy"
    ));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePropelPistonStoredEnergyPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        UpdatePropelPistonStoredEnergyPacket::pos,
        ByteBufCodecs.VAR_INT,
        UpdatePropelPistonStoredEnergyPacket::energy,
        UpdatePropelPistonStoredEnergyPacket::new
    );

    @Override
    public Type<UpdatePropelPistonStoredEnergyPacket> type() {
        return UpdatePropelPistonStoredEnergyPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        Level level = player.level();
        if (!(level.getBlockEntity(this.pos) instanceof PropelPistonBlockEntity propelPiston)) return;
        propelPiston.updateStoredEnergy(this.energy);
    }
}
