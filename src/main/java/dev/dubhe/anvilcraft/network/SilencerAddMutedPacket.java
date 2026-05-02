package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public record SilencerAddMutedPacket(Identifier soundId) implements IServerboundPacket {
    public static final Type<SilencerAddMutedPacket> TYPE = IPacket.type(AnvilCraft.of("silencer_add_muted"));
    public static final StreamCodec<ByteBuf, SilencerAddMutedPacket> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        SilencerAddMutedPacket::soundId,
        SilencerAddMutedPacket::new
    );

    @Override
    public Type<SilencerAddMutedPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.containerMenu instanceof ActiveSilencerMenu menu) {
            menu.addSound(this.soundId);
        }
    }
}
