package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record AutoEnchantingTableLevelPacket(int level) implements IServerboundPacket {
    public static final Type<AutoEnchantingTableLevelPacket> TYPE = IPacket.type(AnvilCraft.of("auto_enchanting_table_level"));
    public static final StreamCodec<ByteBuf, AutoEnchantingTableLevelPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        AutoEnchantingTableLevelPacket::level,
        AutoEnchantingTableLevelPacket::new
    );

    @Override
    public Type<AutoEnchantingTableLevelPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.hasContainerOpen()) return;
        if (!(player.containerMenu instanceof AutoEnchantingTableMenu menu)) return;
        menu.setLiquidLevel(this.level);
    }
}
