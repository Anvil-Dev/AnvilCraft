package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.EmberGrindstoneMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record EmberGrindstoneSyncPacket(int index) implements IServerboundPacket {
    public static final Type<EmberGrindstoneSyncPacket> TYPE = IPacket.type(AnvilCraft.of("ember_grindstone_sync"));
    public static final StreamCodec<ByteBuf, EmberGrindstoneSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        EmberGrindstoneSyncPacket::index,
        EmberGrindstoneSyncPacket::new
    );

    @Override
    public Type<EmberGrindstoneSyncPacket> type() {
        return EmberGrindstoneSyncPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.hasContainerOpen()) return;
        if (!(player.containerMenu instanceof EmberGrindstoneMenu menu)) return;
        menu.setSelectedEnchantment(this.index);
    }
}
