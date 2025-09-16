package dev.dubhe.anvilcraft.api.saved;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.property.component.CrateStorage;
import it.unimi.dsi.fastutil.doubles.Double2ObjectLinkedOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class ShulkerCrateStorage extends SavedData {
    public static final double CURRENT_VERSION = 0;

    private static final Double2ObjectLinkedOpenHashMap<UnaryOperator<CompoundTag>> DATA_FIXERS;

    static {
        @SuppressWarnings("UnnecessaryLocalVariable")
        Double2ObjectLinkedOpenHashMap<UnaryOperator<CompoundTag>> cache = new Double2ObjectLinkedOpenHashMap<>();
        DATA_FIXERS = cache;
    }

    private final Map<UUID, CrateStorage> crates;
    private static final ShulkerCrateStorage CLIENT_STORAGE_COPY = new ShulkerCrateStorage();

    public ShulkerCrateStorage() {
        this.crates = new HashMap<>();
    }

    public static ShulkerCrateStorage get() {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                //noinspection ConstantConditions - 现在主世界已加载
                DimensionDataStorage storage = overworld.getDataStorage();
                return storage.computeIfAbsent(new Factory<>(ShulkerCrateStorage::new, ShulkerCrateStorage::load), AnvilCraft.MOD_ID);
            }
        }
        return CLIENT_STORAGE_COPY;
    }

    public static ShulkerCrateStorage load(CompoundTag nbt, HolderLookup.Provider registries) {
        ShulkerCrateStorage storage = new ShulkerCrateStorage();
        nbt = ShulkerCrateStorage.applyDataFixer(nbt);
        storage.readStorages(nbt, registries);
        return storage;
    }

    private void readStorages(CompoundTag nbt, HolderLookup.Provider registries) {
        for (var entry : nbt.getList("Data", Tag.TAG_COMPOUND)) {
            if (!(entry instanceof CompoundTag entryTag)) return;
            UUID id = entryTag.getUUID("id");
            CrateStorage.CODEC.codec().decode(registries.createSerializationContext(NbtOps.INSTANCE), entryTag.get("contents"))
                .ifSuccess(pair -> this.crates.put(id, pair.getFirst()))
                .ifError(pair -> this.crates.put(id, new CrateStorage(id)));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        this.saveStorages(nbt, registries);
        this.saveVersion(nbt);
        return nbt;
    }

    private void saveStorages(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag data = new ListTag();
        for (var entry : this.crates.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("id", entry.getKey());
            entryTag.put(
                "contents",
                CrateStorage.CODEC.codec().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), entry.getValue())
                    .getOrThrow()
            );
            data.add(entryTag);
        }
        nbt.put("Data", data);
    }

    private void saveVersion(CompoundTag nbt) {
        nbt.putDouble("Version", CURRENT_VERSION);
    }

    private static CompoundTag applyDataFixer(CompoundTag nbt) {
        double version = nbt.getDouble("Version");
        if (version == CURRENT_VERSION) return nbt;
        SortedSet<Double> keys = DATA_FIXERS.keySet();
        if (keys.first() != 0) keys = keys.reversed();
        for (double key : keys) {
            if (key >= CURRENT_VERSION) return nbt;
            nbt = DATA_FIXERS.get(key).apply(nbt);
        }
        return nbt;
    }
}
