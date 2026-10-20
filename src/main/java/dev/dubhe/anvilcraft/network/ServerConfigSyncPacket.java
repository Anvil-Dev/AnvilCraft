package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.ConfigSync;

/** NeoForge 1.21's built-in config payload can only be sent during login configuration. */
public record ServerConfigSyncPacket(String fileName, byte[] contents) implements IClientboundPacket {
    public static final Type<ServerConfigSyncPacket> TYPE = IPacket.type(AnvilCraft.of("server_config_sync"));
    public static final StreamCodec<ByteBuf, ServerConfigSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ServerConfigSyncPacket::fileName,
        ByteBufCodecs.BYTE_ARRAY, ServerConfigSyncPacket::contents,
        ServerConfigSyncPacket::new
    );

    @Override
    public Type<ServerConfigSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        ConfigSync.receiveSyncedConfig(this.contents, this.fileName);
    }
}
