package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.InspectionSupport;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public record InspectionStateChangedPacket(Identifier id, boolean state) implements IClientboundPacket {
    public static final Type<InspectionStateChangedPacket> TYPE = IPacket.type(AnvilCraft.of("inspection_state"));
    public static final StreamCodec<ByteBuf, InspectionStateChangedPacket> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        InspectionStateChangedPacket::id,
        ByteBufCodecs.BOOL,
        InspectionStateChangedPacket::state,
        InspectionStateChangedPacket::new
    );

    @Override
    public Type<InspectionStateChangedPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        InspectionSupport.INSTANCE.changeStateClient(this.id, this.state);
    }
}
