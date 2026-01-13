package dev.dubhe.anvilcraft.saved.sc.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import dev.dubhe.anvilcraft.saved.datafixer.DataFixers;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.recover.RecoverEntry;
import dev.dubhe.anvilcraft.util.recover.RecoverStation;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Getter(AccessLevel.PRIVATE)
public class ServerSCStorages extends SavedData {
    public static final MapCodec<ServerSCStorages> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, ServerSCStorage.CODEC.codec())
            .fieldOf("storages")
            .forGetter(ServerSCStorages::getStorages),
        RecoverStation.codec(ServerSCStorage.CODEC)
            .fieldOf("recover")
            .forGetter(ServerSCStorages::getRecover)
    ).apply(ins, ServerSCStorages::new));
    private static final ResourceLocation FIXERS_ID = AnvilCraft.of("sc_storages_fixers");
    private static final double CURRENT_VERSION = 0.0;
    private final Map<UUID, ServerSCStorage> storages;
    private final RecoverStation<ServerSCStorage> recover = RecoverStation.create(AnvilCraft.CONFIG.containerStorageRecoverMaxSize);

    static {
        ServerSCStorages.registerDataFixers();
    }

    public ServerSCStorages() {
        this.storages = new HashMap<>();
    }

    private ServerSCStorages(Map<UUID, ServerSCStorage> storages, RecoverStation<ServerSCStorage> recover) {
        this.storages = new HashMap<>(storages);
        this.recover.sync(recover);
    }

    public static ServerSCStorages get() {
        if (!Util.isServer()) throw new IllegalStateException("Try to get server-side storages in client-side");
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(
            new Factory<>(ServerSCStorages::new, ServerSCStorages::load),
            AnvilCraft.MOD_ID.concat("_sc_storages")
        );
    }

    public Optional<ServerSCStorage> get(@Nullable UUID uuid) {
        return Optional.ofNullable(uuid).map(this.storages::get);
    }

    public ServerSCStorage getOrCreate(UUID uuid) {
        int oldSize = this.storages.size();
        var result = this.storages.computeIfAbsent(uuid, ServerSCStorage::new);
        if (this.storages.size() != oldSize) this.setDirty();
        return result;
    }

    public void create(UUID id) {
        this.storages.computeIfAbsent(id, ServerSCStorage::new);
        this.setDirty();
    }

    public UUID create() {
        var id = UUID.randomUUID();
        while (this.storages.containsKey(id)) {
            id = UUID.randomUUID();
        }
        this.storages.put(id, new ServerSCStorage(id));
        this.setDirty();
        return id;
    }

    private static void registerDataFixers() {
        DataFixers.registerFixer(FIXERS_ID);
    }

    private static ServerSCStorages load(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt = DataFixers.fixData(FIXERS_ID, CURRENT_VERSION, nbt.getDouble("version"), nbt, registries);
        ServerSCStorages storages = new ServerSCStorages();
        ListTag storagesRaw = nbt.getList("storages", Tag.TAG_COMPOUND);
        for (Tag storage : storagesRaw) {
            var result = ServerSCStorage.CODEC.decoder()
                .decode(registries.createSerializationContext(NbtOps.INSTANCE), storage)
                .getOrThrow()
                .getFirst();
            storages.storages.put(result.getId(), result);
        }
        ListTag recover = nbt.getList("recover", Tag.TAG_COMPOUND);
        for (Tag storage : recover) {
            var result = ServerSCStorage.CODEC.decoder()
                .decode(registries.createSerializationContext(NbtOps.INSTANCE), storage)
                .getOrThrow()
                .getFirst();
            storages.recover.getEntries().add(new RecoverEntry<>(result.getId(), result));
        }
        return storages;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putDouble("version", CURRENT_VERSION);

        ListTag storages = new ListTag();
        for (ServerSCStorage storage : this.storages.values()) {
            CodecUtil.encodeStart(
                ServerSCStorage.CODEC,
                registries.createSerializationContext(NbtOps.INSTANCE),
                storage
            ).map(Util::<CompoundTag>cast).ifSuccess(storages::add);
        }
        nbt.put("storages", storages);

        ListTag recover = new ListTag();
        for (var entry : this.recover.getEntries()) {
            CodecUtil.encodeStart(
                ServerSCStorage.CODEC,
                registries.createSerializationContext(NbtOps.INSTANCE),
                entry.value()
            ).map(Util::<CompoundTag>cast).ifSuccess(recover::add);
        }
        nbt.put("recover", recover);

        return nbt;
    }

    public void sync2C(ServerLevel level, BlockPos pos, UUID id) {
        ServerSCStorages.sync2C(
            this.storages.computeIfAbsent(id, ServerSCStorage::new),
            level.registryAccess(),
            payload -> PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), payload)
        );
    }

    public void sync2C(ServerPlayer player, UUID id) {
        ServerSCStorages.sync2C(
            this.storages.computeIfAbsent(id, ServerSCStorage::new),
            player.serverLevel().registryAccess(),
            payload -> PacketDistributor.sendToPlayer(player, payload)
        );
    }

    private static void sync2C(ServerSCStorage storage, RegistryAccess registries, Consumer<CustomPacketPayload> sender) {
        var id = storage.getId();
        PacketSplitter.INSTANCE.split(
            ShulkerContainerPackets.UpgradesSync.TYPE,
            ShulkerContainerPackets.UpgradesSync.STREAM_CODEC,
            new ShulkerContainerPackets.UpgradesSync(id, storage.getUpgrades()),
            registries,
            sender
        );
        PacketSplitter.INSTANCE.split(
            ShulkerContainerPackets.EntriesSync.TYPE,
            ShulkerContainerPackets.EntriesSync.STREAM_CODEC,
            new ShulkerContainerPackets.EntriesSync(id, storage.getEntries()),
            registries,
            sender
        );
        PacketSplitter.INSTANCE.split(
            ShulkerContainerPackets.CategoriesSync.TYPE,
            ShulkerContainerPackets.CategoriesSync.STREAM_CODEC,
            new ShulkerContainerPackets.CategoriesSync(id, storage.getCategories()),
            registries,
            sender
        );
    }

    public void sync2C(ServerPlayer player) {
        this.sync2C(player.serverLevel().registryAccess());
    }

    public void sync2C(RegistryAccess registries) {
        PacketSplitter.INSTANCE.split(
            ShulkerContainerPackets.StoragesIdSync.TYPE,
            ShulkerContainerPackets.StoragesIdSync.STREAM_CODEC,
            new ShulkerContainerPackets.StoragesIdSync(this.getIDs(), this.getRecoverableIDs()),
            registries,
            PacketDistributor::sendToAllPlayers
        );
    }

    public @Unmodifiable Set<UUID> getIDs() {
        return Set.copyOf(this.storages.keySet());
    }

    public @Unmodifiable Set<UUID> getRecoverableIDs() {
        return this.recover.recoverableIds();
    }

    // 命令

    public boolean removeStorage(UUID id, RegistryAccess registries) {
        ServerSCStorage removed = this.storages.remove(id);
        if (removed != null) {
            this.recover.removed(id, removed);
            this.sync2C(registries);
        }
        this.setDirty();
        return removed != null;
    }

    public boolean recover(UUID id, RegistryAccess registries) {
        var recoveredOp = this.recover.recover(id);
        if (recoveredOp.isEmpty()) return false;
        var recovered = recoveredOp.get();
        this.storages.put(recovered.id(), recovered.value());
        this.sync2C(registries);
        this.setDirty();
        return true;
    }

    public void clearRecover() {
        this.recover.clear();
        this.setDirty();
        PacketDistributor.sendToAllPlayers(new ShulkerContainerPackets.RecoverClear());
    }
}
