package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 结构磁盘预览请求包（C2S）
 * 客户端需要渲染磁盘3D预览时，向服务端请求结构NBT数据。
 * 服务端读取结构文件后，通过 StructurePreviewResponsePacket 返回调色板和方块列表。
 */
public record StructurePreviewRequestPacket(UUID structureUuid, String structureFile) implements IServerboundPacket {
    public static final Type<StructurePreviewRequestPacket> TYPE = IPacket.type(
        AnvilCraft.of("structure_preview_request")
    );

    public static final StreamCodec<ByteBuf, StructurePreviewRequestPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        StructurePreviewRequestPacket::structureUuid,
        ByteBufCodecs.STRING_UTF8,
        StructurePreviewRequestPacket::structureFile,
        StructurePreviewRequestPacket::new
    );

    @Override
    public Type<StructurePreviewRequestPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ServerPlayer serverPlayer = Util.cast(player);
        ServerLevel serverLevel = serverPlayer.level();
        if (this.structureFile.isEmpty()) {
            return;
        }

        // 服务端读取结构文件，提取预览数据
        CompoundTag previewData = StructureLoadUtil.loadPreviewData(serverLevel, this.structureFile);
        if (previewData == null) {
            AnvilCraft.LOGGER.warn(
                "Failed to load preview data for structure: {} (file: {})",
                this.structureUuid, this.structureFile
            );
            return;
        }

        // 发送预览数据回客户端
        serverPlayer.connection.send(
            new StructurePreviewResponsePacket(this.structureUuid, previewData)
        );
    }
}
