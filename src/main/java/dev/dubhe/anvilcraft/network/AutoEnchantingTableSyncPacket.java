package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record AutoEnchantingTableSyncPacket(int index, boolean select) implements IServerboundPacket {
    public static final Type<AutoEnchantingTableSyncPacket> TYPE = IPacket.type(AnvilCraft.of("auto_enchanting_table_sync"));
    public static final StreamCodec<ByteBuf, AutoEnchantingTableSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        AutoEnchantingTableSyncPacket::index,
        ByteBufCodecs.BOOL,
        AutoEnchantingTableSyncPacket::select,
        AutoEnchantingTableSyncPacket::new
    );

    @Override
    public Type<AutoEnchantingTableSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.hasContainerOpen()) return;
        if (!(player.containerMenu instanceof AutoEnchantingTableMenu menu)) return;
        if (this.select) {
            menu.select(this.index);
        } else {
            menu.unselect(this.index);
        }
    }
}
