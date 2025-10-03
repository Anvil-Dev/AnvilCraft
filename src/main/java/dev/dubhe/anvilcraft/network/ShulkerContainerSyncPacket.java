package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.api.container.ContainerStorages;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.UUID;

public record ShulkerContainerSyncPacket(ContainerStorage storage) implements CustomPacketPayload {
    public static final Type<ShulkerContainerSyncPacket> TYPE = new Type<>(AnvilCraft.of("shulker_container_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerContainerSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ContainerStorage.STREAM_CODEC,
        ShulkerContainerSyncPacket::storage,
        ShulkerContainerSyncPacket::new
    );
    public static final IPayloadHandler<ShulkerContainerSyncPacket> HANDLER = ShulkerContainerSyncPacket::bidirectionalHandler;

    public ShulkerContainerSyncPacket(UUID uuid) {
        this(ContainerStorages.get().getOrCreateStorage(uuid));
    }

    @Override
    public Type<ShulkerContainerSyncPacket> type() {
        return TYPE;
    }

    private void bidirectionalHandler(IPayloadContext ctx) {
        ContainerStorage storage = ContainerStorages.get().getOrCreateStorage(this.storage.getId());
        ctx.enqueueWork(() -> storage.sync(this.storage));
    }
}
