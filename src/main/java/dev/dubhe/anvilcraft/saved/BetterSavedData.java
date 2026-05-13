package dev.dubhe.anvilcraft.saved;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Predicate;

public abstract class BetterSavedData extends SavedData {
    protected BetterSavedData() {
        this.registerDataFixers();
    }

    protected abstract void registerDataFixers();

    protected static <T extends BetterSavedData> T get(SavedDataType<T> type, T clientCopy) {
        if (Util.isServer()) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel overworld = server.overworld();
                SavedDataStorage storage = overworld.getDataStorage();
                return storage.computeIfAbsent(type);
            }
        }
        return clientCopy;
    }

    public void sync2C() {
        if (!Util.isServer()) return;
        this.sync2C(ServerLifecycleHooks.getCurrentServer().registryAccess());
    }

    public void sync2C(RegistryAccess registryAccess) {
        this.sync2C(this.createPacket(registryAccess), registryAccess);
    }

    private <T extends CustomPacketPayload> void sync2C(Packet<T> packet, RegistryAccess registryAccess) {
        if (!Util.isServer()) return;
        PacketSplitter.INSTANCE.split(
            packet.type(),
            packet.codec().cast(),
            packet.packet(),
            registryAccess,
            PacketDistributor::sendToAllPlayers
        );
    }

    protected abstract Packet<? extends CustomPacketPayload> createPacket(RegistryAccess registryAccess);

    @SuppressWarnings("RedundantRecordConstructor")
    protected record Packet<T extends CustomPacketPayload>(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        T packet
    ) {
        public Packet {
        }
    }

    /**
     * 生成一个随机的UUID
     *
     * @param containedChecker 判断随机的UUID是否已被使用（尽管概率极低）
     * @return 随机生成的UUID
     */
    protected static UUID generate(Predicate<UUID> containedChecker) {
        var id = UUID.randomUUID();
        while (containedChecker.test(id)) {
            id = UUID.randomUUID();
        }
        return id;
    }
}
