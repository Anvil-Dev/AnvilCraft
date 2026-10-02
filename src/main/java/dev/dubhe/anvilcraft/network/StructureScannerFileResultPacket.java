package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.building.BlueprintClientFiles;
import dev.dubhe.anvilcraft.util.StructureFileTransfer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public record StructureScannerFileResultPacket(UUID id, String error, int total, int offset, byte[] bytes)
    implements IClientboundPacket {
    public static final Type<StructureScannerFileResultPacket> TYPE = IPacket.type(AnvilCraft.of("structure_scanner_file_result"));
    public static final StreamCodec<FriendlyByteBuf, StructureScannerFileResultPacket> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeUUID(packet.id);
            buffer.writeUtf(packet.error, 512);
            buffer.writeVarInt(packet.total);
            buffer.writeVarInt(packet.offset);
            buffer.writeByteArray(packet.bytes);
        },
        buffer -> new StructureScannerFileResultPacket(buffer.readUUID(), buffer.readUtf(512), buffer.readVarInt(),
            buffer.readVarInt(), buffer.readByteArray(StructureFileTransfer.CHUNK_BYTES))
    );

    @Override
    public Type<StructureScannerFileResultPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        BlueprintClientFiles.receive(this);
    }
}
