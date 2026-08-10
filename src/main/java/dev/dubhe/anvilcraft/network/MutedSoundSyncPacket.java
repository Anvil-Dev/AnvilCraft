package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.ActiveSilencerScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record MutedSoundSyncPacket(List<Identifier> sounds) implements IClientboundPacket {
    public static final Type<MutedSoundSyncPacket> TYPE = IPacket.type(AnvilCraft.of("muted_sound_sync"));
    public static final StreamCodec<ByteBuf, MutedSoundSyncPacket> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MutedSoundSyncPacket::sounds,
        MutedSoundSyncPacket::new
    );

    @Override
    public Type<MutedSoundSyncPacket> type() {
        return MutedSoundSyncPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof ActiveSilencerScreen screen)) return;
        screen.handleSync(this.sounds);
    }
}
