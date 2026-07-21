package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.network.RedstoneWirePowerSyncPacket;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端显示用的导线功率缓存。
 *
 * <p>功率是服务端网络管理器的派生值，不写入方块状态或存档。客户端只保留非零值，
 * 未收到同步时按零处理；使用并发映射是因为区块重建可能在渲染线程读取颜色时发生。</p>
 */
public final class RedstoneWireClientPowerCache {
    private static final ConcurrentHashMap<Long, Byte> POWERS = new ConcurrentHashMap<>();
    private static final Long2ObjectOpenHashMap<LongOpenHashSet> POSITIONS_BY_CHUNK = new Long2ObjectOpenHashMap<>();
    private static volatile Level cachedLevel;

    private RedstoneWireClientPowerCache() {
    }

    /** 返回指定客户端世界中导线的显示功率；尚未同步时返回零。 */
    public static int get(Level level, BlockPos pos) {
        if (level == null || cachedLevel != level) {
            return 0;
        }
        Byte value = POWERS.get(pos.asLong());
        return value == null ? 0 : Byte.toUnsignedInt(value);
    }

    /** 供区块渲染区域查询当前客户端世界的功率。 */
    public static int getCurrent(BlockPos pos) {
        return get(cachedLevel, pos);
    }

    /** 更新单个位置，返回其显示颜色是否发生变化。 */
    public static boolean update(Level level, BlockPos pos, int power) {
        ensureLevel(level);
        LongOpenHashSet dirtySections = new LongOpenHashSet();
        int clampedPower = Math.clamp(power, 0, 15);
        if (clampedPower == 0) {
            remove(pos.asLong(), dirtySections);
        } else {
            put(pos, clampedPower, dirtySections);
        }
        return !dirtySections.isEmpty();
    }

    /** 应用一个区块的功率同步，并返回需要重建渲染的区段集合。 */
    public static LongOpenHashSet apply(Level level, RedstoneWirePowerSyncPacket packet) {
        ensureLevel(level);
        long chunkPos = packet.chunkPos();
        LongOpenHashSet dirtySections = new LongOpenHashSet();
        if (packet.replace()) {
            clearChunkInternal(chunkPos, dirtySections);
        }
        for (RedstoneWirePowerSyncPacket.PowerGroup group : packet.groups()) {
            int power = group.power();
            for (int packed : group.positions()) {
                BlockPos pos = RedstoneWirePowerSyncPacket.unpack(chunkPos, packed);
                if (power == 0) {
                    remove(pos.asLong(), dirtySections);
                } else {
                    put(pos, power, dirtySections);
                }
            }
        }
        return dirtySections;
    }

    /** 清理客户端不再加载的区块，避免已移除导线的坐标长期留在缓存中。 */
    public static void clearChunk(Level level, ChunkPos chunkPos) {
        if (level == null || cachedLevel != level) {
            return;
        }
        clearChunkInternal(chunkPos.toLong(), null);
    }

    /** 清理客户端世界切换时的全部派生数据。 */
    public static void clear(Level level) {
        if (level != null && cachedLevel != level) {
            return;
        }
        cachedLevel = null;
        POWERS.clear();
        POSITIONS_BY_CHUNK.clear();
    }

    private static void ensureLevel(Level level) {
        if (cachedLevel == level) {
            return;
        }
        cachedLevel = null;
        POWERS.clear();
        POSITIONS_BY_CHUNK.clear();
        cachedLevel = level;
    }

    private static void put(BlockPos pos, int power, LongOpenHashSet dirtySections) {
        long packedPos = pos.asLong();
        byte value = (byte) power;
        Byte old = POWERS.put(packedPos, value);
        if (old == null) {
            addIndex(pos);
            if (value != 0) {
                markDirty(pos, dirtySections);
            }
        } else if (old.byteValue() != value) {
            markDirty(pos, dirtySections);
        }
    }

    private static void remove(long packedPos, LongOpenHashSet dirtySections) {
        Byte old = POWERS.remove(packedPos);
        if (old == null) {
            return;
        }
        BlockPos pos = BlockPos.of(packedPos);
        long chunkPos = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        LongOpenHashSet positions = POSITIONS_BY_CHUNK.get(chunkPos);
        if (positions != null) {
            positions.remove(packedPos);
            if (positions.isEmpty()) {
                POSITIONS_BY_CHUNK.remove(chunkPos);
            }
        }
        markDirty(pos, dirtySections);
    }

    private static void addIndex(BlockPos pos) {
        long chunkPos = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        POSITIONS_BY_CHUNK.computeIfAbsent(chunkPos, ignored -> new LongOpenHashSet()).add(pos.asLong());
    }

    private static void clearChunkInternal(long chunkPos, LongOpenHashSet dirtySections) {
        LongOpenHashSet positions = POSITIONS_BY_CHUNK.remove(chunkPos);
        if (positions == null) {
            return;
        }
        for (LongIterator iterator = positions.iterator(); iterator.hasNext();) {
            long packedPos = iterator.nextLong();
            POWERS.remove(packedPos);
            if (dirtySections != null) {
                markDirty(BlockPos.of(packedPos), dirtySections);
            }
        }
    }

    private static void markDirty(BlockPos pos, LongOpenHashSet dirtySections) {
        if (dirtySections != null) {
            dirtySections.add(SectionPos.asLong(pos));
        }
    }
}
