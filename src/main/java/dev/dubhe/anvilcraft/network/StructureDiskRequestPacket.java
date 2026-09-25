package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/** 请求结构磁盘的完整结构数据，保留尺寸、方块实体 NBT 等识别信息。 */
public record StructureDiskRequestPacket(String file) implements IServerboundPacket {
    private static final long REQUEST_COOLDOWN_MS = 2000;
    private static final Map<ServerPlayer, Long> LAST_REQUEST = new WeakHashMap<>();
    public static final Type<StructureDiskRequestPacket> TYPE = IPacket.type(AnvilCraft.of("structure_disk_request"));
    public static final StreamCodec<ByteBuf, StructureDiskRequestPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, StructureDiskRequestPacket::file, StructureDiskRequestPacket::new
    );

    @Override
    public Type<StructureDiskRequestPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || StructureLoadUtil.isInvalidStructureFile(this.file)) return;
        long now = System.currentTimeMillis();
        Long last = LAST_REQUEST.get(serverPlayer);
        if (last != null && now - last < REQUEST_COOLDOWN_MS) return;
        LAST_REQUEST.put(serverPlayer, now);
        var tag = StructureLoadUtil.readStructureFileOnServer(serverPlayer.level(), this.file);
        serverPlayer.connection.send(new StructureDiskResponsePacket(this.file, tag));
    }
}
