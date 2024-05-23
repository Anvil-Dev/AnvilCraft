package dev.dubhe.anvilcraft.api.property;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

@Getter
public class SyncedBooleanProperty extends WatchablePropertyDelegate<Boolean> implements SyncedProperty<Boolean> {
    private final UUID syncId;

    public SyncedBooleanProperty(boolean value) {
        super(value);
        syncId = UUID.randomUUID();
        SyncedPropertyManager.getInstance().register(syncId, this);
    }

    @Override
    protected void onChanged(Boolean oldValue, Boolean newValue) {
        super.onChanged(oldValue, newValue);
        //requireSync();
    }

    @Override
    public void synced(FriendlyByteBuf value) {
        this.value = value.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(get());
    }
}
