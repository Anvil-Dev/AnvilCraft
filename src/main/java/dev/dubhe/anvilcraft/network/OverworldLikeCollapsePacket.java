package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/** Starts or resumes the client-only white collapse flash. */
public record OverworldLikeCollapsePacket(int elapsedTicks) implements IClientboundPacket {
    public static final Type<OverworldLikeCollapsePacket> TYPE = IPacket.type(AnvilCraft.of("overworld_like_collapse"));
    public static final StreamCodec<ByteBuf, OverworldLikeCollapsePacket> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        OverworldLikeCollapsePacket::new,
        OverworldLikeCollapsePacket::elapsedTicks
    );

    @Override
    public Type<OverworldLikeCollapsePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        OverworldLikeClientState.beginCollapse(elapsedTicks);
    }
}
