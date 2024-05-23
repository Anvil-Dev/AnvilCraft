package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.api.network.Packet;
import dev.dubhe.anvilcraft.api.property.SyncedProperty;
import dev.dubhe.anvilcraft.api.property.SyncedPropertyManager;
import dev.dubhe.anvilcraft.init.ModNetworks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class PropertySyncPacket implements Packet {
    private final SyncedProperty<?> property;

    public PropertySyncPacket(SyncedProperty<?> property1) {
        this.property = property1;
    }

    public PropertySyncPacket(FriendlyByteBuf buf) {
        property = SyncedPropertyManager.getInstance().callPropertySync(buf);
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.PROPERTY_SYNC;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeUUID(property.getSyncId());
        property.encode(buf);
    }

    @Override
    public void handler() {

    }
}
