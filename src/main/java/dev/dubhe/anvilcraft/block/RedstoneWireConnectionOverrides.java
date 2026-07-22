package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** 仅保存玩家手动编辑过的红石导线端口，每个位置用一个字节表示。 */
final class RedstoneWireConnectionOverrides extends SavedData {
    private static final String DATA_NAME = AnvilCraft.MOD_ID + "_redstone_wire_connections";
    private static final String WIRES_KEY = "Wires";
    private static final String POS_KEY = "Pos";
    private static final String FLAGS_KEY = "Flags";
    private static final int DIRECTION_MASK = 0x0F;
    private static final int HIDDEN_SHIFT = 4;

    private static final Factory<RedstoneWireConnectionOverrides> FACTORY = new Factory<>(
        RedstoneWireConnectionOverrides::new,
        RedstoneWireConnectionOverrides::load
    );

    /** 低四位为强制显示端口，高四位为强制隐藏并断开端口。 */
    private final Long2ByteOpenHashMap entries = new Long2ByteOpenHashMap();

    private RedstoneWireConnectionOverrides() {
    }

    static RedstoneWireConnectionOverrides get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static RedstoneWireConnectionOverrides load(CompoundTag root, HolderLookup.Provider registries) {
        RedstoneWireConnectionOverrides result = new RedstoneWireConnectionOverrides();
        ListTag wires = root.getList(WIRES_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < wires.size(); index++) {
            CompoundTag wire = wires.getCompound(index);
            byte flags = wire.getByte(FLAGS_KEY);
            if (flags != 0) {
                result.entries.put(wire.getLong(POS_KEY), flags);
            }
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
        return this.setFlags(pos, (flags & ~DIRECTION_MASK) | (mask & DIRECTION_MASK));
    }

    boolean setHidden(long pos, int index, boolean hidden) {
        int flags = Byte.toUnsignedInt(this.entries.get(pos));
        int bit = 1 << (index + HIDDEN_SHIFT);
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

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag wires = new ListTag();
        for (Long2ByteMap.Entry entry : this.entries.long2ByteEntrySet()) {
            CompoundTag wire = new CompoundTag();
            wire.putLong(POS_KEY, entry.getLongKey());
            wire.putByte(FLAGS_KEY, entry.getByteValue());
            wires.add(wire);
        }
        root.put(WIRES_KEY, wires);
        return root;
    }
}
