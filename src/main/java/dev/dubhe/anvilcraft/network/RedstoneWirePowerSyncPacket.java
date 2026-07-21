package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireClientPowerCache;
import io.netty.handler.codec.DecoderException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** 按区块同步自定义红石导线的显示功率。 */
public record RedstoneWirePowerSyncPacket(long chunkPos, boolean replace, List<PowerGroup> groups)
    implements IClientboundPacket {
    private static final int MAX_GROUPS = 16;
    private static final int MAX_POSITIONS = 65_536;

    public static final Type<RedstoneWirePowerSyncPacket> TYPE = IPacket.type(
        AnvilCraft.of("redstone_wire_power_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, RedstoneWirePowerSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RedstoneWirePowerSyncPacket decode(FriendlyByteBuf buffer) {
            long chunkPos = buffer.readLong();
            boolean replace = buffer.readBoolean();
            int groupCount = buffer.readVarInt();
            if (groupCount < 0 || groupCount > MAX_GROUPS) {
                throw new DecoderException("Invalid redstone wire power group count: " + groupCount);
            }
            List<PowerGroup> groups = new ArrayList<>(groupCount);
            int positionCount = 0;
            for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
                int power = buffer.readUnsignedByte();
                if (power > 15) {
                    throw new DecoderException("Invalid redstone wire power: " + power);
                }
                int count = buffer.readVarInt();
                if (count < 0 || count > MAX_POSITIONS - positionCount) {
                    throw new DecoderException("Invalid redstone wire power position count: " + count);
                }
                int[] positions = new int[count];
                for (int index = 0; index < count; index++) {
                    positions[index] = buffer.readUnsignedMedium();
                }
                positionCount += count;
                groups.add(new PowerGroup(power, positions));
            }
            return new RedstoneWirePowerSyncPacket(chunkPos, replace, List.copyOf(groups));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, RedstoneWirePowerSyncPacket packet) {
            if (packet.groups().size() > MAX_GROUPS) {
                throw new IllegalArgumentException("Too many redstone wire power groups");
            }
            buffer.writeLong(packet.chunkPos());
            buffer.writeBoolean(packet.replace());
            buffer.writeVarInt(packet.groups().size());
            int positionCount = 0;
            for (PowerGroup group : packet.groups()) {
                if (group.power() < 0 || group.power() > 15) {
                    throw new IllegalArgumentException("Invalid redstone wire power: " + group.power());
                }
                if (group.positions().length > MAX_POSITIONS - positionCount) {
                    throw new IllegalArgumentException("Too many redstone wire power positions");
                }
                buffer.writeByte(group.power());
                buffer.writeVarInt(group.positions().length);
                for (int position : group.positions()) {
                    if ((position & ~0xFFFFFF) != 0) {
                        throw new IllegalArgumentException("Invalid packed redstone wire position: " + position);
                    }
                    buffer.writeMedium(position);
                }
                positionCount += group.positions().length;
            }
        }
    };

    @Override
    public Type<RedstoneWirePowerSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!player.level().isClientSide()) {
            return;
        }
        var dirtySections = RedstoneWireClientPowerCache.apply(player.level(), this);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != player.level()) {
            return;
        }
        for (long section : dirtySections) {
            minecraft.levelRenderer.setSectionDirty(
                SectionPos.x(section),
                SectionPos.y(section),
                SectionPos.z(section)
            );
        }
    }

    /** 将世界坐标压缩为区块内 x/z 和 16 位有符号 y。 */
    public static int pack(BlockPos pos) {
        return ((pos.getY() & 0xFFFF) << 8)
            | ((pos.getZ() & 15) << 4)
            | (pos.getX() & 15);
    }

    /** 从区块坐标和压缩位置恢复世界坐标。 */
    public static BlockPos unpack(long chunkPos, int packed) {
        int y = packed >>> 8;
        if ((y & 0x8000) != 0) {
            y |= ~0xFFFF;
        }
        int chunkX = net.minecraft.world.level.ChunkPos.getX(chunkPos);
        int chunkZ = net.minecraft.world.level.ChunkPos.getZ(chunkPos);
        return new BlockPos((chunkX << 4) + (packed & 15), y, (chunkZ << 4) + ((packed >>> 4) & 15));
    }

    public record PowerGroup(int power, int[] positions) {
    }
}
