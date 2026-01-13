package dev.dubhe.anvilcraft.saved.sc.client;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ClientSCStorages {
    private static final Map<UUID, ClientSCStorage> STORAGES = new HashMap<>();
    private static final Set<UUID> RECOVERABLE_IDS = new HashSet<>();

    public static Optional<ClientSCStorage> get(@Nullable UUID id) {
        return Optional.ofNullable(id).map(ClientSCStorages.STORAGES::get);
    }

    public static ClientSCStorage getOrCreate(UUID id) {
        return ClientSCStorages.STORAGES.computeIfAbsent(id, ClientSCStorage::new);
    }

    public static void create(UUID id) {
        ClientSCStorages.STORAGES.put(id, new ClientSCStorage(id));
    }

    public static @Unmodifiable Set<UUID> getIDs() {
        return ClientSCStorages.STORAGES.keySet();
    }

    public static @Unmodifiable Set<UUID> getRecoverableIDs() {
        return ClientSCStorages.RECOVERABLE_IDS;
    }

    public static void sync(Set<UUID> storageIds, Set<UUID> recoverableIds) {
        var old = Map.copyOf(ClientSCStorages.STORAGES);
        ClientSCStorages.STORAGES.clear();
        for (UUID storageId : storageIds) {
            ClientSCStorages.STORAGES.put(storageId, old.getOrDefault(storageId, new ClientSCStorage(storageId)));
        }

        ClientSCStorages.RECOVERABLE_IDS.clear();
        ClientSCStorages.RECOVERABLE_IDS.addAll(recoverableIds);
    }

    public static void clearRecover() {
        ClientSCStorages.RECOVERABLE_IDS.clear();
    }

    public static void clear() {
        ClientSCStorages.STORAGES.clear();
        ClientSCStorages.RECOVERABLE_IDS.clear();
    }
}
