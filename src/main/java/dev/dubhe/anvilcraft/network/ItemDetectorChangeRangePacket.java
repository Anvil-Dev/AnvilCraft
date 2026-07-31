package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record ItemDetectorChangeRangePacket(int range) implements IServerboundPacket {
    public static final Type<ItemDetectorChangeRangePacket> TYPE = IPacket.type(AnvilCraft.of("item_detector_change_range"));
    public static final StreamCodec<ByteBuf, ItemDetectorChangeRangePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        ItemDetectorChangeRangePacket::range,
        ItemDetectorChangeRangePacket::new
    );

    @Override
    public Type<ItemDetectorChangeRangePacket> type() {
        return ItemDetectorChangeRangePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof ItemDetectorMenu menu)) return;
        menu.setRange(this.range);
    }
}
