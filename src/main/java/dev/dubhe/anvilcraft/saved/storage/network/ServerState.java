package dev.dubhe.anvilcraft.saved.storage.network;

import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class ServerState {
    private static final Map<UUID, ServerState> STATES = new HashMap<>();

    public static ServerState get(UUID id) {
        return ServerState.STATES.computeIfAbsent(id, ServerState::new);
    }

    private final UUID id;
    private final Map<Integer, UnlimitedItemStack> changes;

    public ServerState(UUID id) {
        this.id = id;
        this.changes = new HashMap<>();
    }

    public BaseStorage getStorage() {
        return Storages.get().get(this.id).orElseThrow(() -> new IllegalStateException("Trying to create state with an unbounded id"));
    }
}
