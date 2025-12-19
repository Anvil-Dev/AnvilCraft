package dev.dubhe.anvilcraft.saved;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class BetterSavedData extends SavedData {
    protected BetterSavedData() {
        this.registerDataFixers();
    }

    protected abstract void registerDataFixers();

    public abstract void read(CompoundTag nbt, HolderLookup.Provider registries);

    @Override
    public abstract CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries);

    protected static <T extends BetterSavedData> T get(String id, Supplier<T> constructor, T clientCopy) {
        if (Util.isServer()) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                // noinspection ConstantConditions - 主世界已加载
                DimensionDataStorage storage = overworld.getDataStorage();
                return storage.computeIfAbsent(
                    new SavedData.Factory<>(constructor, BetterSavedData.getLoader(constructor)),
                    AnvilCraft.MOD_ID.concat("_").concat(id)
                );
            }
        }
        return clientCopy;
    }

    private static <T extends BetterSavedData> BiFunction<CompoundTag, HolderLookup.Provider, T> getLoader(Supplier<T> constructor) {
        return (nbt, registries) -> Util.run(constructor.get(), data -> data.read(nbt, registries));
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
