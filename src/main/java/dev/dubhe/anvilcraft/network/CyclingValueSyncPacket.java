package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.component.CyclingValueHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record CyclingValueSyncPacket(int index, String name) implements IServerboundPacket {
    public static final Type<CyclingValueSyncPacket> TYPE = IPacket.type(AnvilCraft.of("cycling_value"));
    public static final StreamCodec<ByteBuf, CyclingValueSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        CyclingValueSyncPacket::index,
        ByteBufCodecs.STRING_UTF8,
        CyclingValueSyncPacket::name,
        CyclingValueSyncPacket::new
    );

    @Override
    public Type<CyclingValueSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.containerMenu instanceof CyclingValueHandler handler) {
            handler.notify(this.index, this.name);
        }
    }
}
