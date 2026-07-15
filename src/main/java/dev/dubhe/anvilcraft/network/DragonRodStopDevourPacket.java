package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.tool.DragonRodItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record DragonRodStopDevourPacket() implements IServerboundPacket {
    public static final Type<DragonRodStopDevourPacket> TYPE = IPacket.type(AnvilCraft.of("dragon_rod_stop_devour"));
    public static final StreamCodec<ByteBuf, DragonRodStopDevourPacket> STREAM_CODEC = StreamCodec.unit(
        new DragonRodStopDevourPacket()
    );

    @Override
    public Type<DragonRodStopDevourPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        DragonRodItem.stopContinuousMode(player);
    }
}
