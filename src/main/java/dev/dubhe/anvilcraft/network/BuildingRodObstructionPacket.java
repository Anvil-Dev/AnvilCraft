package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.building.BuildingRodObstructionHighlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record BuildingRodObstructionPacket(List<Integer> entities) implements IClientboundPacket {
    public static final Type<BuildingRodObstructionPacket> TYPE = IPacket.type(AnvilCraft.of("building_rod_obstruction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuildingRodObstructionPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), BuildingRodObstructionPacket::entities, BuildingRodObstructionPacket::new);

    @Override
    public Type<BuildingRodObstructionPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        BuildingRodObstructionHighlight.show(player.level(), this.entities);
    }
}
