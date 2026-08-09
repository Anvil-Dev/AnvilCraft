package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record TeslaRemoveFilterPacket(String id, String arg) implements IServerboundPacket {
    public static final Type<TeslaRemoveFilterPacket> TYPE = IPacket.type(AnvilCraft.of("tesla_filter_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeslaRemoveFilterPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        TeslaRemoveFilterPacket::id,
        ByteBufCodecs.STRING_UTF8,
        TeslaRemoveFilterPacket::arg,
        TeslaRemoveFilterPacket::new
    );

    @Override
    public Type<TeslaRemoveFilterPacket> type() {
        return TeslaRemoveFilterPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.containerMenu instanceof TeslaTowerMenu menu) {
            menu.removeFilter(this.id, this.arg);
        }
    }
}
