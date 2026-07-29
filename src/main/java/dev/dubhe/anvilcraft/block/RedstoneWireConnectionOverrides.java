package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// 仅保存玩家手动编辑过的红石导线端口，每个位置用一个字节表示
final class RedstoneWireConnectionOverrides extends SavedData {
    private static final String LEGACY_FILE_NAME = AnvilCraft.MOD_ID + "_redstone_wire_connections.dat";
    private static final String WIRES_KEY = "wires";
    private static final String POS_KEY = "pos";
    private static final String FLAGS_KEY = "flags";
    private static final String LEGACY_MIGRATION_KEY = "legacy_migration_complete";
    private static final String LEGACY_WIRES_KEY = "Wires";
    private static final String LEGACY_POS_KEY = "Pos";
    private static final String LEGACY_FLAGS_KEY = "Flags";
    private static final int DIRECTION_MASK = 0x0F;
    private static final int HIDDEN_SHIFT = 4;

    private static final Codec<RedstoneWireConnectionOverrides> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Entry.CODEC.listOf().fieldOf(WIRES_KEY).forGetter(RedstoneWireConnectionOverrides::toEntries),
        Codec.BOOL.optionalFieldOf(LEGACY_MIGRATION_KEY, false)
            .forGetter(overrides -> overrides.legacyMigrationComplete)
    ).apply(instance, RedstoneWireConnectionOverrides::new));

    private static final SavedDataType<RedstoneWireConnectionOverrides> TYPE = new SavedDataType<>(
        AnvilCraft.of("redstone_wire_connections"),
        RedstoneWireConnectionOverrides::new,
        RedstoneWireConnectionOverrides.CODEC
    );

    /// 低四位为强制显示端口，高四位为强制隐藏并断开端口
    private final Long2ByteOpenHashMap entries = new Long2ByteOpenHashMap();
    private boolean legacyMigrationComplete;

    private RedstoneWireConnectionOverrides() {
    }

    private RedstoneWireConnectionOverrides(List<Entry> entries, boolean legacyMigrationComplete) {
        for (Entry entry : entries) {
            if (entry.flags() != 0) this.entries.put(entry.pos(), (byte) entry.flags());
        }
        this.legacyMigrationComplete = legacyMigrationComplete;
    }

    static RedstoneWireConnectionOverrides get(ServerLevel level) {
        SavedDataStorage storage = level.getDataStorage();
        RedstoneWireConnectionOverrides result = storage.get(RedstoneWireConnectionOverrides.TYPE);
        boolean create = result == null;
        if (result == null) {
            result = new RedstoneWireConnectionOverrides();
        }
        if (!result.legacyMigrationComplete) {
            RedstoneWireConnectionOverrides legacy = loadLegacy(level, storage);
            if (legacy != null) {
                result.mergeMissing(legacy);
            }
            result.legacyMigrationComplete = true;
            result.setDirty();
        }
        if (create) {
            storage.set(RedstoneWireConnectionOverrides.TYPE, result);
        }
        return result;
    }

    private static @Nullable RedstoneWireConnectionOverrides loadLegacy(
        ServerLevel level,
        SavedDataStorage storage
    ) {
        for (Path dataFile : legacyDataFiles(level)) {
            if (!Files.isRegularFile(dataFile)) {
                continue;
            }
            try {
                CompoundTag root = storage.readTagFromDisk(dataFile, null, 0);
                CompoundTag data = root.getCompound("data").orElse(root);
                if (!data.contains(WIRES_KEY) && !data.contains(LEGACY_WIRES_KEY)) {
                    continue;
                }
                RedstoneWireConnectionOverrides result = loadEntries(data);
                AnvilCraft.LOGGER.info("Migrated redstone wire connection overrides from {}", dataFile);
                return result;
            } catch (Exception exception) {
                AnvilCraft.LOGGER.warn("Failed to migrate redstone wire connection overrides from {}", dataFile, exception);
            }
        }
        return null;
    }

    private static List<Path> legacyDataFiles(ServerLevel level) {
        Path worldFolder = level.getServer().getWorldPath(LevelResource.ROOT);
        Path currentDimensionFolder = DimensionType.getStorageFolder(level.dimension(), worldFolder);
        Path currentDataFolder = currentDimensionFolder.resolve("data");
        List<Path> result = new ArrayList<>();
        Path currentLegacyFile = currentDataFolder.resolve(LEGACY_FILE_NAME);
        result.add(currentLegacyFile);

        Path legacyDimensionFolder;
        if (level.dimension() == Level.OVERWORLD) {
            legacyDimensionFolder = worldFolder;
        } else if (level.dimension() == Level.NETHER) {
            legacyDimensionFolder = worldFolder.resolve("DIM-1");
        } else if (level.dimension() == Level.END) {
            legacyDimensionFolder = worldFolder.resolve("DIM1");
        } else {
            legacyDimensionFolder = currentDimensionFolder;
        }
        Path legacyFile = legacyDimensionFolder.resolve("data").resolve(LEGACY_FILE_NAME);
        if (!legacyFile.equals(currentLegacyFile)) {
            result.add(legacyFile);
        }
        return result;
    }

    private void mergeMissing(RedstoneWireConnectionOverrides legacy) {
        for (Long2ByteMap.Entry entry : legacy.entries.long2ByteEntrySet()) {
            if (!this.entries.containsKey(entry.getLongKey())) {
                this.entries.put(entry.getLongKey(), entry.getByteValue());
            }
        }
    }

    private static RedstoneWireConnectionOverrides loadEntries(CompoundTag data) {
        RedstoneWireConnectionOverrides result = new RedstoneWireConnectionOverrides();
        ListTag wires = data.contains(WIRES_KEY)
            ? data.getListOrEmpty(WIRES_KEY)
            : data.getListOrEmpty(LEGACY_WIRES_KEY);
        for (int index = 0; index < wires.size(); index++) {
            CompoundTag wire = wires.getCompoundOrEmpty(index);
            String posKey = wire.contains(POS_KEY) ? POS_KEY : LEGACY_POS_KEY;
            String flagsKey = wire.contains(FLAGS_KEY) ? FLAGS_KEY : LEGACY_FLAGS_KEY;
            if (!wire.contains(posKey) || !wire.contains(flagsKey)) {
                continue;
            }
            byte flags = wire.getByteOr(flagsKey, (byte) 0);
            if (flags != 0) {
                result.entries.put(wire.getLongOr(posKey, 0), flags);
            }
        }
        return result;
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
            Codec.LONG.fieldOf(POS_KEY).forGetter(Entry::pos),
            Codec.INT.fieldOf(FLAGS_KEY).forGetter(Entry::flags)
        ).apply(instance, Entry::new));
    }
}
