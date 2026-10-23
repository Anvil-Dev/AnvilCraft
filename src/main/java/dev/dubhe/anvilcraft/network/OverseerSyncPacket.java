package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.client.overseer.OverseerMapData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** An empty chunk map removes the source; other updates replace that source's loaded chunks. */
public record OverseerSyncPacket(ResourceKey<Level> dimension, BlockPos source, Map<Long, Integer> chunks)
    implements IClientboundPacket {
    public static final Type<OverseerSyncPacket> TYPE = IPacket.type(AnvilCraft.of("overseer_sync"));
    public static final StreamCodec<FriendlyByteBuf, OverseerSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OverseerSyncPacket decode(FriendlyByteBuf buffer) {
            ResourceKey<Level> dimension = buffer.readResourceKey(Registries.DIMENSION);
            BlockPos source = BlockPos.STREAM_CODEC.decode(buffer);
            int count = buffer.readVarInt();
            if (count < 0 || count > 81) throw new IllegalArgumentException("Invalid overseer chunk count: " + count);
            Map<Long, Integer> chunks = new HashMap<>();
            for (int i = 0; i < count; i++) {
                chunks.put(buffer.readLong(), (int) buffer.readUnsignedByte());
            }
            return new OverseerSyncPacket(dimension, source, Map.copyOf(chunks));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, OverseerSyncPacket packet) {
            buffer.writeResourceKey(packet.dimension());
            BlockPos.STREAM_CODEC.encode(buffer, packet.source());
            buffer.writeVarInt(packet.chunks().size());
            packet.chunks().forEach((chunk, flags) -> {
                buffer.writeLong(chunk);
                buffer.writeByte(flags);
            });
        }
    };

    public static OverseerSyncPacket of(LoadChuckData data) {
        Map<Long, Integer> chunks = new HashMap<>();
        for (ChunkPos chunk : data.getChunkPosList()) {
            int flags = data.isNeedRandomTick() ? 1 : 0;
            chunks.put(chunk.toLong(), flags);
        }
        return new OverseerSyncPacket(data.getServerLevel().dimension(), data.getCenterPos(), Map.copyOf(chunks));
    }

    @Override
    public Type<OverseerSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        OverseerMapData.update(this);
    }
}
