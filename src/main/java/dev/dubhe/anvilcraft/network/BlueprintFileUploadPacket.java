package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintUploadTracker;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** 客户端蓝图文件的一个上传分块;服务端按玩家聚合,完成后解析并写入所持磁盘。 */
public record BlueprintFileUploadPacket(
    UUID sessionId,
    String fileName,
    int chunkIndex,
    int totalChunks,
    int totalBytes,
    boolean autoRotate,
    byte[] bytes
) implements IServerboundPacket {
    /** 单个分块的字节上限。 */
    public static final int CHUNK_BYTES = 30_000;

    public static final Type<BlueprintFileUploadPacket> TYPE = IPacket.type(
        AnvilCraft.of("blueprint_file_upload")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintFileUploadPacket> STREAM_CODEC =
        StreamCodec.of(BlueprintFileUploadPacket::write, BlueprintFileUploadPacket::read);

    private static void write(RegistryFriendlyByteBuf buffer, BlueprintFileUploadPacket packet) {
        UUIDUtil.STREAM_CODEC.encode(buffer, packet.sessionId);
        buffer.writeUtf(packet.fileName, BlueprintUploadTracker.MAX_FILE_NAME_LENGTH);
        buffer.writeVarInt(packet.chunkIndex);
        buffer.writeVarInt(packet.totalChunks);
        buffer.writeVarInt(packet.totalBytes);
        buffer.writeBoolean(packet.autoRotate);
        buffer.writeByteArray(packet.bytes);
    }

    private static BlueprintFileUploadPacket read(RegistryFriendlyByteBuf buffer) {
        return new BlueprintFileUploadPacket(
            UUIDUtil.STREAM_CODEC.decode(buffer),
            buffer.readUtf(BlueprintUploadTracker.MAX_FILE_NAME_LENGTH),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readByteArray(CHUNK_BYTES)
        );
    }

    @Override
    public Type<BlueprintFileUploadPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        BlueprintUploadTracker.accept(
            serverPlayer,
            this.sessionId,
            this.fileName,
            this.chunkIndex,
            this.totalChunks,
            this.totalBytes,
            this.autoRotate,
            this.bytes
        );
    }
}
