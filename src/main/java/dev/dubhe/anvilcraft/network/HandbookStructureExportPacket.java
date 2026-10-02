package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record HandbookStructureExportPacket(Identifier location, int totalBytes, int offset, byte[] data) implements IServerboundPacket {
    public static final int CHUNK_BYTES = 24 * 1024;
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    public static final Type<HandbookStructureExportPacket> TYPE = IPacket.type(AnvilCraft.of("handbook_structure_export"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HandbookStructureExportPacket> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeIdentifier(payload.location());
            buffer.writeVarInt(payload.totalBytes());
            buffer.writeVarInt(payload.offset());
            buffer.writeByteArray(payload.data());
        },
        buffer -> new HandbookStructureExportPacket(buffer.readIdentifier(), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readByteArray(CHUNK_BYTES))
    );

    @Override
    public Type<HandbookStructureExportPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) HandbookStructureExportHandler.handle(this, serverPlayer);
    }
}
