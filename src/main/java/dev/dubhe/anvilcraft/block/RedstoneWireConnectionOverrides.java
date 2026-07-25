package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/// 仅保存玩家手动编辑过的红石导线端口，每个位置用一个字节表示
final class RedstoneWireConnectionOverrides extends SavedData {
    private static final int DIRECTION_MASK = 0x0F;
    private static final int HIDDEN_SHIFT = 4;

    private static final Codec<RedstoneWireConnectionOverrides> CODEC = Entry.CODEC
        .listOf()
        .fieldOf("wires")
        .xmap(RedstoneWireConnectionOverrides::new, RedstoneWireConnectionOverrides::toEntries)
        .codec();

    private static final SavedDataType<RedstoneWireConnectionOverrides> TYPE = new SavedDataType<>(
        AnvilCraft.of("redstone_wire_connections"),
        RedstoneWireConnectionOverrides::new,
        RedstoneWireConnectionOverrides.CODEC
    );

    /// 低四位为强制显示端口，高四位为强制隐藏并断开端口
    private final Long2ByteOpenHashMap entries = new Long2ByteOpenHashMap();

    private RedstoneWireConnectionOverrides() {
    }

    private RedstoneWireConnectionOverrides(List<Entry> entries) {
        for (Entry entry : entries) {
            if (entry.flags() != 0) this.entries.put(entry.pos(), (byte) entry.flags());
        }
    }

    static RedstoneWireConnectionOverrides get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RedstoneWireConnectionOverrides.TYPE);
    }

    private List<Entry> toEntries() {
        List<Entry> result = new ArrayList<>(this.entries.size());
        for (Long2ByteMap.Entry entry : this.entries.long2ByteEntrySet()) {
            result.add(new Entry(entry.getLongKey(), Byte.toUnsignedInt(entry.getByteValue())));
        }
        return result;
    }

    int forcedMask(long pos) {
        return Byte.toUnsignedInt(this.entries.get(pos)) & DIRECTION_MASK;
    }

    int hiddenMask(long pos) {
        return Byte.toUnsignedInt(this.entries.get(pos)) >>> HIDDEN_SHIFT;
    }

    boolean setForcedMask(long pos, int mask) {
        int flags = Byte.toUnsignedInt(this.entries.get(pos));
        return this.setFlags(pos, flags & ~DIRECTION_MASK | mask & DIRECTION_MASK);
    }

    boolean setHidden(long pos, int index, boolean hidden) {
        int flags = Byte.toUnsignedInt(this.entries.get(pos));
        int bit = 1 << index + HIDDEN_SHIFT;
        return this.setFlags(pos, hidden ? flags | bit : flags & ~bit);
    }

    void clear(long pos) {
        if (this.entries.remove(pos) != 0) {
            this.setDirty();
        }
    }

    private boolean setFlags(long pos, int flags) {
        byte value = (byte) flags;
        byte previous;
        if (value == 0) {
            previous = this.entries.remove(pos);
        } else {
            previous = this.entries.put(pos, value);
        }
        if (previous == value) {
            return false;
        }
        this.setDirty();
        return true;
    }

    /// 单个导线位置的端口覆写记录
    private record Entry(long pos, int flags) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
            Codec.INT.fieldOf("flags").forGetter(Entry::flags)
        ).apply(instance, Entry::new));
    }
}
