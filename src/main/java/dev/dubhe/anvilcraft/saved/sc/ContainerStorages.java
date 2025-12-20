package dev.dubhe.anvilcraft.saved.sc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import dev.dubhe.anvilcraft.saved.datafixer.DataFixers;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.recover.RecoverStation;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter(AccessLevel.PRIVATE)
public class ContainerStorages extends BetterSavedData {
    public static final MapCodec<ContainerStorages> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, ContainerStorage.CODEC.codec())
            .fieldOf("storages")
            .forGetter(ContainerStorages::getStorages),
        RecoverStation.codec(ContainerStorage.CODEC)
            .fieldOf("recover")
            .forGetter(ContainerStorages::getRecover)
    ).apply(ins, ContainerStorages::new));
    private static final ContainerStorages CLIENT_COPY = new ContainerStorages();
    private static final ResourceLocation FIXERS_ID = AnvilCraft.of("sc_storages_fixers");
    private static final double CURRENT_VERSION = 0.0;
    private final Map<UUID, ContainerStorage> storages;
    private final RecoverStation<ContainerStorage> recover = RecoverStation.create(AnvilCraft.CONFIG.containerStorageRecoverMaxSize);

    public ContainerStorages() {
        this.storages = new HashMap<>();
    }

    private ContainerStorages(Map<UUID, ContainerStorage> storages, RecoverStation<ContainerStorage> recover) {
        this.storages = new HashMap<>(storages);
        this.recover.sync(recover);
    }

    public static ContainerStorages get() {
        return BetterSavedData.get("sc_storages", ContainerStorages::new, ContainerStorages.CLIENT_COPY);
    }

    public Optional<ContainerStorage> get(@Nullable UUID uuid) {
        return Optional.ofNullable(uuid).map(this.storages::get);
    }

    public ContainerStorage getOrCreate(UUID uuid) {
        return this.storages.computeIfAbsent(uuid, ContainerStorage::new);
    }

    public UUID create() {
        return BetterSavedData.generate(this.storages::containsKey);
    }

    public void syncFromServer(Set<UUID> ids, Set<UUID> recoverableIds) {
        var oldStorages = Map.copyOf(this.storages);
        this.storages.clear();
        for (UUID id : ids) {
            this.storages.put(id, oldStorages.getOrDefault(id, new ContainerStorage(id)));
        }
        this.recover.sync(recoverableIds);
    }

    @Override
    protected void registerDataFixers() {
        DataFixers.registerFixer(FIXERS_ID);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt = DataFixers.fixData(FIXERS_ID, CURRENT_VERSION, nbt, registries);
        ContainerStorages.CODEC.compressedDecode(registries.createSerializationContext(NbtOps.INSTANCE), nbt)
            .result()
            .ifPresent(storages -> {
                this.storages.clear();
                this.storages.putAll(storages.storages);
                this.recover.sync(storages.recover);
            });
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        var result = CodecUtil.encode(ContainerStorages.CODEC, this, registries.createSerializationContext(NbtOps.INSTANCE), nbt);
        nbt = Util.cast(result.getOrThrow());
        nbt.putDouble("version", CURRENT_VERSION);
        return nbt;
    }

    @Override
    protected Packet<ShulkerContainerPackets.StoragesSync> createPacket(RegistryAccess registryAccess) {
        return new Packet<>(
            ShulkerContainerPackets.StoragesSync.TYPE,
            ShulkerContainerPackets.StoragesSync.STREAM_CODEC,
            new ShulkerContainerPackets.StoragesSync(this.getIDs(), this.getRecoverableIDs())
        );
    }

    public void sync2C(ServerLevel level, BlockPos pos, UUID id) {
        PacketSplitter.INSTANCE.split(
            ShulkerContainerPackets.StorageSync.TYPE,
            ShulkerContainerPackets.StorageSync.STREAM_CODEC,
            new ShulkerContainerPackets.StorageSync(this.getOrCreate(id)),
            level.registryAccess(),
            payload -> PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), payload)
        );
    }

    public Set<UUID> getIDs() {
        return Set.copyOf(this.storages.keySet());
    }

    public Set<UUID> getRecoverableIDs() {
        return this.recover.recoverableIds();
    }

    // 命令

    public boolean removeStorage(UUID id, RegistryAccess registries) {
        try {
            ContainerStorage removed = this.storages.remove(id);
            if (removed != null) {
                this.recover.removed(id, removed);
                this.sync2C(registries);
            }
            this.setDirty();
            return removed != null;
        } catch (UnsupportedOperationException e) {
            throw new IllegalStateException("Unexpected unsupported remove operation in ContainerStorages.storages");
        }
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

    public void clearRecoverFromCommand() {
        this.clearRecover();
        this.setDirty();
        PacketDistributor.sendToAllPlayers(new ShulkerContainerPackets.RecoverClear());
    }

    public void clearRecover() {
        this.recover.clear();
    }
}
