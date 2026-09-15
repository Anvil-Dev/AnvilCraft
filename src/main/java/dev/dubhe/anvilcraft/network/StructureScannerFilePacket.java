package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.StructureFileTransfer;
import dev.dubhe.anvilcraft.util.StructureScannerFiles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public record StructureScannerFilePacket(int containerId, UUID id, String name, int total, int offset, byte[] bytes)
    implements IServerboundPacket {
    public static final Type<StructureScannerFilePacket> TYPE = IPacket.type(AnvilCraft.of("structure_scanner_file"));
    public static final StreamCodec<FriendlyByteBuf, StructureScannerFilePacket> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeVarInt(packet.containerId);
            buffer.writeUUID(packet.id);
            buffer.writeUtf(packet.name, 128);
            buffer.writeVarInt(packet.total);
            buffer.writeVarInt(packet.offset);
            buffer.writeByteArray(packet.bytes);
        },
        buffer -> new StructureScannerFilePacket(buffer.readVarInt(), buffer.readUUID(), buffer.readUtf(128),
            buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(StructureFileTransfer.CHUNK_BYTES))
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
