package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public record SilencerRemoveMutedPacket(Identifier soundId) implements IServerboundPacket {
    public static final Type<SilencerRemoveMutedPacket> TYPE = IPacket.type(AnvilCraft.of("silencer_remove_muted"));
    public static final StreamCodec<ByteBuf, SilencerRemoveMutedPacket> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        SilencerRemoveMutedPacket::soundId,
        SilencerRemoveMutedPacket::new
    );

    @Override
    public Type<SilencerRemoveMutedPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.containerMenu instanceof ActiveSilencerMenu menu) {
            menu.removeSound(this.soundId);
        }
    }
}
