package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Synchronizes the gravity sources of the player's current level. */
public record GravitySourcesSyncPacket(
    boolean replace,
    List<SourceData> sources,
    List<BlockPos> removed
) implements IClientboundPacket {
    public static final Type<GravitySourcesSyncPacket> TYPE = IPacket.type(AnvilCraft.of("gravity_sources_sync"));
    public static final StreamCodec<FriendlyByteBuf, GravitySourcesSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GravitySourcesSyncPacket decode(FriendlyByteBuf buffer) {
            boolean replace = buffer.readBoolean();
            int sourceCount = buffer.readVarInt();
            List<SourceData> sources = new ArrayList<>(sourceCount);
            for (int i = 0; i < sourceCount; i++) {
                sources.add(SourceData.decode(buffer));
            }
            int removedCount = buffer.readVarInt();
            List<BlockPos> removed = new ArrayList<>(removedCount);
            for (int i = 0; i < removedCount; i++) {
                removed.add(BlockPos.STREAM_CODEC.decode(buffer));
            }
            return new GravitySourcesSyncPacket(replace, List.copyOf(sources), List.copyOf(removed));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, GravitySourcesSyncPacket packet) {
            buffer.writeBoolean(packet.replace());
            buffer.writeVarInt(packet.sources().size());
            for (SourceData source : packet.sources()) {
                source.encode(buffer);
            }
            buffer.writeVarInt(packet.removed().size());
            for (BlockPos id : packet.removed()) {
                BlockPos.STREAM_CODEC.encode(buffer, id);
            }
        }
    };

    @Override
    public Type<GravitySourcesSyncPacket> type() {
        return GravitySourcesSyncPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        GravityManager.applyNetworkSync(player.level(), this.replace, this.sources, this.removed);
    }

    public record SourceData(
        BlockPos id,
        double centerX,
        double centerY,
        double centerZ,
        double strength,
        int radius,
        double bodyRadius
    ) {
        public static SourceData from(GravityManager.GravitySource source) {
            return new SourceData(
                source.id(),
                source.center().x,
                source.center().y,
                source.center().z,
                source.type().strength(),
                source.type().radius(),
                source.type().bodyRadius()
            );
        }

        public GravityManager.@Nullable GravitySource toSource() {
            GravityManager.GravitySourceType type = new GravityManager.GravitySourceType(
                this.strength, this.radius, this.bodyRadius
            );
            Vec3 center = new Vec3(this.centerX, this.centerY, this.centerZ);
            if (!type.isValid()
                || !Double.isFinite(this.centerX)
                || !Double.isFinite(this.centerY)
                || !Double.isFinite(this.centerZ)) {
                return null;
            }
            return new GravityManager.GravitySource(this.id.immutable(), center, type);
        }

        private static SourceData decode(FriendlyByteBuf buffer) {
            return new SourceData(
                BlockPos.STREAM_CODEC.decode(buffer),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readDouble()
            );
        }

        private void encode(FriendlyByteBuf buffer) {
            BlockPos.STREAM_CODEC.encode(buffer, this.id);
            buffer.writeDouble(this.centerX);
            buffer.writeDouble(this.centerY);
            buffer.writeDouble(this.centerZ);
            buffer.writeDouble(this.strength);
            buffer.writeVarInt(this.radius);
            buffer.writeDouble(this.bodyRadius);
        }
    }
}
