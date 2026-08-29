package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public record PowerGridSyncChunkPacket(
    int gridId,
    String level,
    BlockPos pos,
    int totalChunks,
    int chunkIndex,
    int generate,
    int consume,
    boolean infinitePower,
    List<PowerComponentInfo> components
) implements IClientboundPacket {
    public static final int MAX_COMPONENTS_PER_PACKET = 256;
    public static final Type<PowerGridSyncChunkPacket> TYPE = new Type<>(AnvilCraft.of("power_grid_sync_chunk"));
    public static final StreamCodec<ByteBuf, PowerGridSyncChunkPacket> STREAM_CODEC = new StreamCodec<>() {
        private final StreamCodec<ByteBuf, List<PowerComponentInfo>> listCodec =
            PowerComponentInfo.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_COMPONENTS_PER_PACKET));

        @Override
        public PowerGridSyncChunkPacket decode(ByteBuf buffer) {
            int gridId = buffer.readInt();
            String level = ByteBufCodecs.STRING_UTF8.decode(buffer);
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
            int totalChunks = ByteBufCodecs.VAR_INT.decode(buffer);
            int chunkIndex = ByteBufCodecs.VAR_INT.decode(buffer);
            int generate = ByteBufCodecs.VAR_INT.decode(buffer);
            int consume = ByteBufCodecs.VAR_INT.decode(buffer);
            boolean infinitePower = buffer.readBoolean();
            List<PowerComponentInfo> components = listCodec.decode(buffer);
            return new PowerGridSyncChunkPacket(
                gridId, level, pos, totalChunks, chunkIndex, generate, consume, infinitePower, components
            );
        }

        @Override
        public void encode(ByteBuf buffer, PowerGridSyncChunkPacket packet) {
            buffer.writeInt(packet.gridId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, packet.level());
            BlockPos.STREAM_CODEC.encode(buffer, packet.pos());
            ByteBufCodecs.VAR_INT.encode(buffer, packet.totalChunks());
            ByteBufCodecs.VAR_INT.encode(buffer, packet.chunkIndex());
            ByteBufCodecs.VAR_INT.encode(buffer, packet.generate());
            ByteBufCodecs.VAR_INT.encode(buffer, packet.consume());
            buffer.writeBoolean(packet.infinitePower());
            listCodec.encode(buffer, packet.components());
        }
    };

    public static void send(PowerGrid grid) {
        for (PowerGridSyncChunkPacket packet : chunks(grid)) {
            PacketDistributor.sendToPlayersTrackingChunk(
                (ServerLevel) grid.getLevel(),
                grid.getLevel().getChunkAt(grid.getPos()).getPos(),
                packet
            );
        }
    }

    public static void sendToAllPlayers(PowerGrid grid) {
        for (PowerGridSyncChunkPacket packet : chunks(grid)) {
            PacketDistributor.sendToAllPlayers(packet);
        }
    }

    public static void sendToPlayer(PowerGrid grid, ServerPlayer player) {
        for (PowerGridSyncChunkPacket packet : chunks(grid)) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    private static PowerGridSyncChunkPacket[] chunks(PowerGrid grid) {
        List<PowerComponentInfo> all = new ArrayList<>();
        SimplePowerGrid simple = new SimplePowerGrid(grid);
        all.addAll(simple.getPowerComponentInfoList());
        int totalChunks = Math.max(1, (all.size() + MAX_COMPONENTS_PER_PACKET - 1) / MAX_COMPONENTS_PER_PACKET);
        int gridId = grid.hashCode();
        String level = grid.getLevel().dimension().location().toString();
        BlockPos pos = grid.getPos();
        int generate = grid.getGenerate();
        int consume = grid.getConsume();
        boolean infinitePower = grid.isHasInfinitePower();
        PowerGridSyncChunkPacket[] packets = new PowerGridSyncChunkPacket[totalChunks];
        for (int i = 0; i < totalChunks; i++) {
            int from = i * MAX_COMPONENTS_PER_PACKET;
            int to = Math.min(all.size(), from + MAX_COMPONENTS_PER_PACKET);
            List<PowerComponentInfo> part = all.subList(from, to);
            packets[i] = new PowerGridSyncChunkPacket(
                gridId, level, pos, totalChunks, i, generate, consume, infinitePower, part
            );
        }
        return packets;
    }

    @Override
    public Type<PowerGridSyncChunkPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        PowerGridSupport.mergeSyncChunk(this);
    }
}
