package dev.dubhe.anvilcraft.api.property;

import dev.dubhe.anvilcraft.network.PropertySyncPacket;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncedPropertyManager {
    @Getter
    private static final SyncedPropertyManager instance = new SyncedPropertyManager();
    private final Map<UUID, SyncedProperty<?>> syncedPropertyMap = new HashMap<>();

    public void init() {
        syncedPropertyMap.clear();
    }

    void register(UUID syncId, SyncedProperty<?> property) {
        syncedPropertyMap.put(syncId, property);
    }

    public void handleIncomingPacket() {

    }

    public SyncedProperty<?> callPropertySync(FriendlyByteBuf buf) {
        SyncedProperty<?> property = syncedPropertyMap.get(buf.readUUID());
        if (property == null) return null;
        property.synced(buf);
        return property;
    }

    public <T> void requireSync(SyncedProperty<T> property) {
        new PropertySyncPacket(property).broadcast();
    }
}
