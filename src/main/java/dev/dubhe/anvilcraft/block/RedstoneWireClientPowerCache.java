package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.network.RedstoneWirePowerSyncPacket;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

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
    private static volatile @Nullable Level cachedLevel;

    private RedstoneWireClientPowerCache() {
    }

    /** 返回指定客户端世界中导线的显示功率；尚未同步时返回零。 */
    public static int get(@Nullable Level level, BlockPos pos) {
        if (level == null || RedstoneWireClientPowerCache.cachedLevel != level) {
            return 0;
        }
        Byte value = RedstoneWireClientPowerCache.POWERS.get(pos.asLong());
        return value == null ? 0 : Byte.toUnsignedInt(value);
    }

    /** 供区块渲染区域查询当前客户端世界的功率。 */
    public static int getCurrent(BlockPos pos) {
        return RedstoneWireClientPowerCache.get(RedstoneWireClientPowerCache.cachedLevel, pos);
    }

    /** 更新单个位置，返回其显示颜色是否发生变化。 */
    public static boolean update(Level level, BlockPos pos, int power) {
        RedstoneWireClientPowerCache.ensureLevel(level);
        LongOpenHashSet dirtySections = new LongOpenHashSet();
        int clampedPower = Math.clamp(power, 0, 15);
        if (clampedPower == 0) {
            RedstoneWireClientPowerCache.remove(pos.asLong(), dirtySections);
        } else {
            RedstoneWireClientPowerCache.put(pos, clampedPower, dirtySections);
        }
        return !dirtySections.isEmpty();
    }

    /** 应用一个区块的功率同步，并返回需要重建渲染的区段集合。 */
    public static LongOpenHashSet apply(Level level, RedstoneWirePowerSyncPacket packet) {
        RedstoneWireClientPowerCache.ensureLevel(level);
        long chunkPos = packet.chunkPos();
        LongOpenHashSet dirtySections = new LongOpenHashSet();
        if (packet.replace()) {
            RedstoneWireClientPowerCache.clearChunkInternal(chunkPos, dirtySections);
        }
        for (RedstoneWirePowerSyncPacket.PowerGroup group : packet.groups()) {
            int power = group.power();
            for (int packed : group.positions()) {
                BlockPos pos = RedstoneWirePowerSyncPacket.unpack(chunkPos, packed);
                if (power == 0) {
                    RedstoneWireClientPowerCache.remove(pos.asLong(), dirtySections);
                } else {
                    RedstoneWireClientPowerCache.put(pos, power, dirtySections);
                }
            }
        }
        return dirtySections;
    }

    /** 清理客户端不再加载的区块，避免已移除导线的坐标长期留在缓存中。 */
    public static void clearChunk(@Nullable Level level, ChunkPos chunkPos) {
        if (level == null || RedstoneWireClientPowerCache.cachedLevel != level) {
            return;
        }
        RedstoneWireClientPowerCache.clearChunkInternal(chunkPos.pack(), null);
    }

    /** 清理客户端世界切换时的全部派生数据。 */
    public static void clear(@Nullable Level level) {
        if (level != null && RedstoneWireClientPowerCache.cachedLevel != level) {
            return;
        }
        RedstoneWireClientPowerCache.cachedLevel = null;
        RedstoneWireClientPowerCache.POWERS.clear();
        RedstoneWireClientPowerCache.POSITIONS_BY_CHUNK.clear();
    }

    private static void ensureLevel(Level level) {
        if (RedstoneWireClientPowerCache.cachedLevel == level) {
            return;
        }
        RedstoneWireClientPowerCache.cachedLevel = null;
        RedstoneWireClientPowerCache.POWERS.clear();
        RedstoneWireClientPowerCache.POSITIONS_BY_CHUNK.clear();
        RedstoneWireClientPowerCache.cachedLevel = level;
    }

    private static void put(BlockPos pos, int power, LongOpenHashSet dirtySections) {
        long packedPos = pos.asLong();
        byte value = (byte) power;
        Byte old = RedstoneWireClientPowerCache.POWERS.put(packedPos, value);
        if (old == null) {
            RedstoneWireClientPowerCache.addIndex(pos);
            if (value != 0) {
                RedstoneWireClientPowerCache.markDirty(pos, dirtySections);
            }
        } else if (old.byteValue() != value) {
            RedstoneWireClientPowerCache.markDirty(pos, dirtySections);
        }
    }

    private static void remove(long packedPos, LongOpenHashSet dirtySections) {
        Byte old = RedstoneWireClientPowerCache.POWERS.remove(packedPos);
        if (old == null) {
            return;
        }
        BlockPos pos = BlockPos.of(packedPos);
        long chunkPos = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        if (RedstoneWireClientPowerCache.POSITIONS_BY_CHUNK.containsKey(chunkPos)) {
            LongOpenHashSet positions = RedstoneWireClientPowerCache.POSITIONS_BY_CHUNK.get(chunkPos);
            positions.remove(packedPos);
            if (positions.isEmpty()) {
                RedstoneWireClientPowerCache.POSITIONS_BY_CHUNK.remove(chunkPos);
            }
        }
        RedstoneWireClientPowerCache.markDirty(pos, dirtySections);
    }

    private static void addIndex(BlockPos pos) {
        long chunkPos = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        RedstoneWireClientPowerCache.POSITIONS_BY_CHUNK.computeIfAbsent(chunkPos, ignored -> new LongOpenHashSet()).add(pos.asLong());
    }

    private static void clearChunkInternal(long chunkPos, @Nullable LongOpenHashSet dirtySections) {
        if (!RedstoneWireClientPowerCache.POSITIONS_BY_CHUNK.containsKey(chunkPos)) return;
        LongOpenHashSet positions = RedstoneWireClientPowerCache.POSITIONS_BY_CHUNK.remove(chunkPos);
        for (LongIterator iterator = positions.iterator(); iterator.hasNext();) {
            long packedPos = iterator.nextLong();
            RedstoneWireClientPowerCache.POWERS.remove(packedPos);
            if (dirtySections != null) {
                RedstoneWireClientPowerCache.markDirty(BlockPos.of(packedPos), dirtySections);
            }
        }
    }

    private static void markDirty(BlockPos pos, @Nullable LongOpenHashSet dirtySections) {
        if (dirtySections != null) {
            dirtySections.add(SectionPos.asLong(pos));
        }
    }
}
