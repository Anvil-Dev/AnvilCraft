package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.recover.RecoverStation;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
public class Storages extends SavedData {
    public static final ResourceLocation ID = AnvilCraft.of("storages");
    public static final SavedData.Factory<Storages> TYPE = new SavedData.Factory<>(Storages::new, Storages::load);
    public static final Storages CLIENT_COPY = new Storages();
    private static final Codec<Map<UUID, BaseStorage<?>>> STORAGES_CODEC = Codec.unboundedMap(
        UUIDUtil.STRING_CODEC,
        BaseStorage.CODEC.codec()
    );
    private static final Codec<RecoverStation<BaseStorage<?>>> RECOVER_CODEC = RecoverStation.codec(BaseStorage.CODEC).codec();
    private final Map<UUID, BaseStorage<?>> storages = new HashMap<>();
    private final RecoverStation<BaseStorage<?>> recover;

    private Storages() {
        this.recover = RecoverStation.create(AnvilCraft.CONFIG.storageRecoverMaxSize);
    }

    private Storages(Map<UUID, BaseStorage<?>> storages, RecoverStation<BaseStorage<?>> recover) {
        this.storages.putAll(storages);
        this.recover = recover;
    }

    private Map<UUID, BaseStorage<?>> getStoragesForCodec() {
        return this.storages;
    }

    private RecoverStation<BaseStorage<?>> getRecoverForCodec() {
        return this.recover;
    }

    public static Storages get() {
        if (!Util.isServer()) {
            return Storages.CLIENT_COPY;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Cannot access Storages when the server was not initialized");
        }

        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(Storages.TYPE, "storages");
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        this.putIfSuccess(STORAGES_CODEC.encodeStart(ops, this.storages), tag, "storages");
        this.putIfSuccess(RECOVER_CODEC.encodeStart(ops, this.recover), tag, "recover");
        return tag;
    }

    private void putIfSuccess(DataResult<Tag> result, CompoundTag tag, String key) {
        result.result().ifPresent(element -> tag.put(key, element));
    }

    public static Storages load(CompoundTag tag, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        Map<UUID, BaseStorage<?>> storages = new HashMap<>();
        RecoverStation<BaseStorage<?>> recover = RecoverStation.create(AnvilCraft.CONFIG.storageRecoverMaxSize);
        if (tag.contains("storages")) {
            STORAGES_CODEC.parse(ops, tag.get("storages")).result().ifPresent(storages::putAll);
        }
        if (tag.contains("recover")) {
            RECOVER_CODEC.parse(ops, tag.get("recover")).result().ifPresent(loaded ->
                recover.getEntries().addAll(loaded.getEntries())
            );
        }
        return new Storages(storages, recover);
    }

    public Optional<BaseStorage<?>> get(UUID id) {
        return Optional.ofNullable(this.storages.get(id));
    }

    public <T extends BaseStorage<?>> Optional<T> get(UUID id, Class<T> clazz) {
        BaseStorage<?> storage = this.storages.get(id);
        if (clazz.isInstance(storage)) {
            return Optional.of(clazz.cast(storage));
        }
        return Optional.empty();
    }

    public void put(BaseStorage<?> storage) {
        this.storages.put(storage.getId(), storage);
        this.setDirty();
    }

    public void remove(UUID id) {
        if (this.storages.remove(id) != null) {
            this.setDirty();
        }
    }

    public <T extends BaseStorage<?>> T getOrCreate(UUID id, Class<T> clazz) {
        BaseStorage<?> storage = this.storages.get(id);
        if (storage == null) {
            T empty = Util.cast(StorageType.find(clazz).newInstance(id));
            this.storages.put(id, empty);
            this.setDirty();
            return empty;
        }
        if (clazz.isInstance(storage)) {
            return clazz.cast(storage);
        }
        throw new IllegalArgumentException(
            "Storage with id '%s' cannot be cast to expected type '%s'. Actual type: %s"
                .formatted(id, clazz.getName(), storage.getClass().getName())
        );
    }
}
