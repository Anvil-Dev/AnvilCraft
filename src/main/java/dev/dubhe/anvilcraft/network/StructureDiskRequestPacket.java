package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 客户端请求结构磁盘对应的 NBT 结构文件。
 *
 * <p>结构磁盘的预览依赖本地 NBT 文件，在纯服务器环境下客户端本地没有该文件，
 * 因此客户端通过本包向服务器请求，服务器读取并回传 {@link StructureDiskResponsePacket}。</p>
 *
 * @param file 结构文件名（形如 {@code name_uuid.nbt}）
 */
public record StructureDiskRequestPacket(String file) implements IServerboundPacket {
    /** 同一玩家请求结构文件的冷却，避免批量探测服务器文件。 */
    private static final long REQUEST_COOLDOWN_MS = 2000;
    private static final Map<ServerPlayer, Long> LAST_REQUEST = new WeakHashMap<>();

    public static final Type<StructureDiskRequestPacket> TYPE = IPacket.type(
        AnvilCraft.of("structure_disk_request")
    );
    public static final StreamCodec<ByteBuf, StructureDiskRequestPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        StructureDiskRequestPacket::file,
        StructureDiskRequestPacket::new
    );

    @Override
    public Type<StructureDiskRequestPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!StructureLoadUtil.isValidStructureFile(this.file)) return;

        long now = System.currentTimeMillis();
        Long last = LAST_REQUEST.put(serverPlayer, now);
        if (last != null && now - last < REQUEST_COOLDOWN_MS) return;

        CompoundTag tag = StructureLoadUtil.readStructureFileOnServer(serverPlayer.serverLevel(), this.file);
        PacketDistributor.sendToPlayer(
            serverPlayer,
            new StructureDiskResponsePacket(this.file, tag)
        );
    }
}
