package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.FrostGrindstoneMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record FrostGrindstoneSyncPacket(int index, boolean select) implements IServerboundPacket {
    public static final Type<FrostGrindstoneSyncPacket> TYPE = IPacket.type(AnvilCraft.of("frost_grindstone_sync"));
    public static final StreamCodec<ByteBuf, FrostGrindstoneSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        FrostGrindstoneSyncPacket::index,
        ByteBufCodecs.BOOL,
        FrostGrindstoneSyncPacket::select,
        FrostGrindstoneSyncPacket::new
    );

    @Override
    public Type<FrostGrindstoneSyncPacket> type() {
        return FrostGrindstoneSyncPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.hasContainerOpen()) return;
        if (!(player.containerMenu instanceof FrostGrindstoneMenu menu)) return;
        if (this.select) {
            menu.select(this.index);
        } else {
            menu.unselect(this.index);
        }
    }
}
