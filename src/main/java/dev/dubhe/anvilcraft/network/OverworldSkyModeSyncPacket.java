package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.MunTravelEventListener;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record OverworldSkyModeSyncPacket(boolean special) implements IServerboundPacket {
    public static final Type<OverworldSkyModeSyncPacket> TYPE = IPacket.type(AnvilCraft.of("overworld_sky_mode_sync"));
    public static final StreamCodec<ByteBuf, OverworldSkyModeSyncPacket> STREAM_CODEC = ByteBufCodecs.BOOL.map(
        OverworldSkyModeSyncPacket::new, OverworldSkyModeSyncPacket::special
    );

    @Override
    public Type<OverworldSkyModeSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) MunTravelEventListener.setSpecialSky(serverPlayer, this.special);
    }
}
