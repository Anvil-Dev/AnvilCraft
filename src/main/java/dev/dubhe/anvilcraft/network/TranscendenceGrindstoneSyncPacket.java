package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.TranscendenceGrindstoneMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record TranscendenceGrindstoneSyncPacket(int index, boolean select) implements IServerboundPacket {
    public static final Type<TranscendenceGrindstoneSyncPacket> TYPE = IPacket.type(
        AnvilCraft.of("transcendence_grindstone_sync")
    );
    public static final StreamCodec<ByteBuf, TranscendenceGrindstoneSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TranscendenceGrindstoneSyncPacket::index,
        ByteBufCodecs.BOOL,
        TranscendenceGrindstoneSyncPacket::select,
        TranscendenceGrindstoneSyncPacket::new
    );

    @Override
    public Type<TranscendenceGrindstoneSyncPacket> type() {
        return TranscendenceGrindstoneSyncPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.hasContainerOpen()) return;
        if (!(player.containerMenu instanceof TranscendenceGrindstoneMenu menu)) return;
        if (this.select) {
            menu.select(this.index);
        } else {
            menu.unselect(this.index);
        }
    }
}
