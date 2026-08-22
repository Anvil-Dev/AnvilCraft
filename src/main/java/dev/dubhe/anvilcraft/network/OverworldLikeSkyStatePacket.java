package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/** Synchronizes the immutable generation and current phase of the orbital sky. */
public record OverworldLikeSkyStatePacket(
    int generation,
    long visualSeed,
    long orbitEpochGameTime,
    OverworldLikeWorldState.Phase phase
) implements IClientboundPacket {
    public static final Type<OverworldLikeSkyStatePacket> TYPE = IPacket.type(AnvilCraft.of("overworld_like_sky_state"));
    public static final StreamCodec<ByteBuf, OverworldLikeSkyStatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        OverworldLikeSkyStatePacket::generation,
        ByteBufCodecs.VAR_LONG,
        OverworldLikeSkyStatePacket::visualSeed,
        ByteBufCodecs.VAR_LONG,
        OverworldLikeSkyStatePacket::orbitEpochGameTime,
        ByteBufCodecs.VAR_INT,
        packet -> packet.phase().ordinal(),
        (generation, visualSeed, orbitEpochGameTime, phase) -> new OverworldLikeSkyStatePacket(
            generation,
            visualSeed,
            orbitEpochGameTime,
            OverworldLikeWorldState.Phase.fromOrdinal(phase)
        )
    );

    @Override
    public Type<OverworldLikeSkyStatePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        OverworldLikeClientState.update(generation, visualSeed, orbitEpochGameTime, phase);
    }
}
