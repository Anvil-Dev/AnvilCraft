package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record IonoCraftBackpackToggleDescentPacket() implements IServerboundPacket {
    public static final Type<IonoCraftBackpackToggleDescentPacket> TYPE = IPacket.type(AnvilCraft.of("ionocraft_backpack_toggle_descent"));
    public static final StreamCodec<ByteBuf, IonoCraftBackpackToggleDescentPacket> STREAM_CODEC = StreamCodec.unit(
        new IonoCraftBackpackToggleDescentPacket()
    );

    @Override
    public Type<IonoCraftBackpackToggleDescentPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            IonoCraftBackpackItem.toggleSlowFalling(serverPlayer);
        }
    }
}
