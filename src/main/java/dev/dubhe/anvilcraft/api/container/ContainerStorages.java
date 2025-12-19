package dev.dubhe.anvilcraft.api.container;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.container.datafixer.StorageDataFixers;
import dev.dubhe.anvilcraft.api.container.recover.RecoverEntry;
import dev.dubhe.anvilcraft.api.container.recover.RecoverStation;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ContainerStorages extends SavedData {
    private static final ContainerStorages CLIENT_STORAGE_COPY = new ContainerStorages();
    private final Map<UUID, ContainerStorage> storages;
    private final RecoverStation recover = RecoverStation.create(AnvilCraft.CONFIG.containerStorageRecoverMaxSize);

    public ContainerStorages() {
        this.storages = new HashMap<>();
    }

    public static ContainerStorages get() {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                // noinspection ConstantConditions - 主世界已加载
                DimensionDataStorage storage = overworld.getDataStorage();
                return storage.computeIfAbsent(
                    new Factory<>(ContainerStorages::new, ContainerStorages::load),
                    AnvilCraft.MOD_ID.concat("_shulker_container_storage")
                );
            }
        }
        return CLIENT_STORAGE_COPY;
    }

    public UUID create() {
        UUID uuid = UUID.randomUUID();
        while (this.storages.containsKey(uuid)) {
            uuid = UUID.randomUUID();
        }
        this.storages.put(uuid, new ContainerStorage(uuid));
        return uuid;
    }

    public Optional<ContainerStorage> getStorage(@Nullable UUID uuid) {
        return Optional.ofNullable(uuid).map(this.storages::get);
    }

    public ContainerStorage getOrCreateStorage(UUID uuid) {
        return this.storages.computeIfAbsent(uuid, ContainerStorage::new);
    }

    public Set<UUID> getContainerIDs() {
        return Set.copyOf(this.storages.keySet());
    }

    public Set<UUID> getRecoverableContainerIDs() {
        return this.recover.recoverableIds();
    }

    public void syncFromServer(Set<UUID> ids, Set<UUID> recoverableIds) {
        var oldStorages = Map.copyOf(this.storages);
        this.storages.clear();
        for (UUID id : ids) {
            this.storages.put(id, oldStorages.getOrDefault(id, new ContainerStorage(id)));
        }
        this.recover.sync(true, recoverableIds);
    }

    public void syncToClient(RegistryAccess registryAccess) {
        PacketSplitter.INSTANCE.split(
            ShulkerContainerPackets.StoragesSync.TYPE,
            ShulkerContainerPackets.StoragesSync.STREAM_CODEC,
            new ShulkerContainerPackets.StoragesSync(this.getContainerIDs(), this.getRecoverableContainerIDs()),
            registryAccess,
            PacketDistributor::sendToAllPlayers
        );
    }

    public void syncToClient(ServerLevel level, BlockPos pos, UUID id) {
        PacketSplitter.INSTANCE.split(
            ShulkerContainerPackets.StorageSync.TYPE,
            ShulkerContainerPackets.StorageSync.STREAM_CODEC,
            new ShulkerContainerPackets.StorageSync(this.getOrCreateStorage(id)),
            level.registryAccess(),
            payload -> PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), payload)
        );
    }

    public static ContainerStorages load(CompoundTag nbt, HolderLookup.Provider registries) {
        ContainerStorages storage = new ContainerStorages();

        StorageDataFixers fixers = ContainerStorages.createFixers(nbt);
        storage.readStorages(nbt.getList("Storages", Tag.TAG_COMPOUND), fixers, registries);
        storage.readStorages(nbt.getList("Recovers", Tag.TAG_COMPOUND), fixers, registries);

        return storage;
    }

    private static StorageDataFixers createFixers(CompoundTag nbt) {
        Object2DoubleMap<ResourceLocation> versions = new Object2DoubleOpenHashMap<>();
        for (String key : nbt.getCompound("Versions").getAllKeys()) {
            versions.put(ResourceLocation.parse(key), nbt.getDouble(key));
        }
        return StorageDataFixers.create(versions);
    }

    private void readStorages(ListTag nbt, StorageDataFixers fixers, HolderLookup.Provider registries) {
        for (var entry : nbt) {
            if (!(entry instanceof CompoundTag entryTag)) return;
            entryTag = fixers.fixData(entryTag);
            UUID id = entryTag.getUUID("id");
            ContainerStorage.CODEC.codec()
                .decode(registries.createSerializationContext(NbtOps.INSTANCE), entryTag.get("contents"))
                .ifSuccess(pair -> this.storages.put(id, pair.getFirst()));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();

        this.saveVersions(nbt, registries);
        this.saveStorages(nbt, "Storages", registries);
        this.saveStorages(nbt, "Recovers", registries);

        return nbt;
    }

    private void saveVersions(CompoundTag nbt, HolderLookup.Provider registries) {
        Object2DoubleMap<ResourceLocation> versions = new Object2DoubleOpenHashMap<>();
        registries.lookup(ModRegistries.FIXER_KEY).ifPresent(lookup -> lookup.listElements().forEach(fixerRef -> versions.merge(
            fixerRef.key().location(),
            fixerRef.value().version(),
            Double::max
        )));
        for (var entry : versions.object2DoubleEntrySet()) {
            nbt.putDouble(entry.getKey().toString(), entry.getDoubleValue());
        }
    }

    private void saveStorages(CompoundTag nbt, String name, HolderLookup.Provider registries) {
        ListTag data = new ListTag();
        for (var entry : this.storages.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("id", entry.getKey());
            entryTag.put(
                "contents",
                ContainerStorage.CODEC.codec()
                    .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), entry.getValue())
                    .getOrThrow()
            );
            data.add(entryTag);
        }
        nbt.put(name, data);
    }

    // 命令

    public boolean removeStorage(UUID id, RegistryAccess registries) {
        try {
            ContainerStorage removed = this.storages.remove(id);
            if (removed != null) {
                this.recover.removed(id, removed);
                this.syncToClient(registries);
            }
            this.setDirty();
            return removed != null;
        } catch (UnsupportedOperationException e) {
            throw new IllegalStateException("Unexpected unsupported remove operation in ContainerStorages.storages");
        }
    }

    public boolean recover(UUID id, RegistryAccess registries) {
        var recoveredOp = this.recover.recover(id);
        if (recoveredOp.isEmpty()) return false;
        RecoverEntry recovered = recoveredOp.get();
        this.storages.put(recovered.id(), recovered.storage());
        this.syncToClient(registries);
        this.setDirty();
        return true;
    }

    public void clearRecoverFromCommand() {
        this.clearRecover();
        this.setDirty();
        PacketDistributor.sendToAllPlayers(new ShulkerContainerPackets.RecoverClear());
    }

    public void clearRecover() {
        this.recover.clear();
    }
}
