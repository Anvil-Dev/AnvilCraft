package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record TeslaAddFilterPacket(String id, String arg) implements IServerboundPacket {
    public static final Type<TeslaAddFilterPacket> TYPE = IPacket.type(AnvilCraft.of("tesla_add_filter"));
    public static final StreamCodec<ByteBuf, TeslaAddFilterPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        TeslaAddFilterPacket::id,
        ByteBufCodecs.STRING_UTF8,
        TeslaAddFilterPacket::arg,
        TeslaAddFilterPacket::new
    );

    @Override
    public Type<TeslaAddFilterPacket> type() {
        return TeslaAddFilterPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.containerMenu instanceof TeslaTowerMenu menu) {
            menu.addFilter(this.id, this.arg);
        }
    }
}
