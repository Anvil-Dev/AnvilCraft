package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.IonocraftBackpackItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record IonocraftBackpackToggleDescentPacket() implements IServerboundPacket {
    public static final Type<IonocraftBackpackToggleDescentPacket> TYPE = IPacket.type(AnvilCraft.of("ionocraft_backpack_toggle_descent"));
    public static final StreamCodec<ByteBuf, IonocraftBackpackToggleDescentPacket> STREAM_CODEC = StreamCodec.unit(
        new IonocraftBackpackToggleDescentPacket()
    );

    @Override
    public Type<IonocraftBackpackToggleDescentPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            IonocraftBackpackItem.toggleSlowFalling(serverPlayer);
        }
    }
}
