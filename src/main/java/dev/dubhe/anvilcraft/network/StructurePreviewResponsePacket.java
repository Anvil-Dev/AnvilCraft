package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.StructureDiskPreviewSupport;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 结构磁盘预览数据同步包（S2C）
 * 服务端响应客户端的预览请求，发送结构NBT中的调色板和方块列表。
 * 客户端收到后缓存到 StructureDiskPreviewSupport，下次 tooltip 渲染时显示3D预览。
 */
public record StructurePreviewResponsePacket(UUID structureUuid, CompoundTag structureData) implements IClientboundPacket {
    public static final Type<StructurePreviewResponsePacket> TYPE = new Type<>(
        AnvilCraft.of("structure_preview_response")
    );

    public static final StreamCodec<ByteBuf, StructurePreviewResponsePacket> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        StructurePreviewResponsePacket::structureUuid,
        ByteBufCodecs.COMPOUND_TAG,
        StructurePreviewResponsePacket::structureData,
        StructurePreviewResponsePacket::new
    );

    @Override
    public Type<StructurePreviewResponsePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        // 将服务端返回的结构数据写入预览缓存
        StructureDiskPreviewSupport.receiveStructureData(this.structureUuid, this.structureData);
    }
}
