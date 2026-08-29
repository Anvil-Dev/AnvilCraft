package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * 服务器返回结构磁盘对应的 NBT 结构文件，供客户端本地预览缓存。
 *
 * @param file 结构文件名
 * @param tag  结构 NBT 数据；文件不存在或读取失败时为 {@code null}
 */
public record StructureDiskResponsePacket(String file, CompoundTag tag) implements IClientboundPacket {
    public static final Type<StructureDiskResponsePacket> TYPE = IPacket.type(
        AnvilCraft.of("structure_disk_response")
    );
    public static final StreamCodec<ByteBuf, StructureDiskResponsePacket> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> {
            ByteBufCodecs.STRING_UTF8.encode(buffer, packet.file());
            FriendlyByteBuf.writeNbt(buffer, packet.tag());
        },
        buffer -> new StructureDiskResponsePacket(
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            FriendlyByteBuf.readNbt(buffer)
        )
    );

    @Override
    public Type<StructureDiskResponsePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        StructureLoadUtil.cacheStructureNbt(this.file, this.tag);
    }
}
