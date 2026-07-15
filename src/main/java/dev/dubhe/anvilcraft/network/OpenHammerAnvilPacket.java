package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record OpenHammerAnvilPacket(int menuSlotId) implements IServerboundPacket {
    public static final Type<OpenHammerAnvilPacket> TYPE = IPacket.type(AnvilCraft.of("open_hammer_anvil"));
    public static final StreamCodec<ByteBuf, OpenHammerAnvilPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        OpenHammerAnvilPacket::menuSlotId,
        OpenHammerAnvilPacket::new
    );

    @Override
    public Type<OpenHammerAnvilPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        AnvilHammerItem.openPortableAnvilFromMenuSlot(player, this.menuSlotId);
    }
}
