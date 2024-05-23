package dev.dubhe.anvilcraft.api.property;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public interface SyncedProperty<T> {
    void synced(FriendlyByteBuf value);

    void encode(FriendlyByteBuf buf);

    default void requireSync() {
        SyncedPropertyManager.getInstance().requireSync(this);
    }

    UUID getSyncId();
}
