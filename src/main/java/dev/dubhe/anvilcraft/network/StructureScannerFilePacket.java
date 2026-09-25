package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.StructureScannerFiles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public record StructureScannerFilePacket(int containerId, UUID id, Action action, String name)
    implements IServerboundPacket {
    public enum Action {
        LIST, IMPORT, EXPORT, RECIPE
    }

    public static final Type<StructureScannerFilePacket> TYPE = IPacket.type(AnvilCraft.of("structure_scanner_file"));
    public static final StreamCodec<FriendlyByteBuf, StructureScannerFilePacket> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeVarInt(packet.containerId);
            buffer.writeUUID(packet.id);
            buffer.writeEnum(packet.action);
            buffer.writeUtf(packet.name, 128);
        },
        buffer -> new StructureScannerFilePacket(buffer.readVarInt(), buffer.readUUID(), buffer.readEnum(Action.class), buffer.readUtf(128))
    );

    @Override
    public Type<StructureScannerFilePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.containerMenu instanceof StructureScannerMenu menu)
            || menu.containerId != this.containerId || !menu.stillValid(player)) return;
        StructureScannerFiles.handle(serverPlayer, menu, this);
    }
}
