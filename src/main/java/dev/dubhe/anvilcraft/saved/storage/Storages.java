package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.recover.RecoverStation;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import javax.annotation.Nullable;

@Getter
public class Storages extends SavedData {
    public static final ResourceLocation ID = AnvilCraft.of("storages");
    public static final SavedData.Factory<Storages> TYPE = new SavedData.Factory<>(Storages::new, Storages::load);
    public static final Storages CLIENT_COPY = new Storages();
    private static @Nullable Storages loading;
    private final Map<UUID, BaseStorage<?>> storages = new HashMap<>();
    private final RecoverStation<BaseStorage<?>> recover;

    private Storages() {
        this.recover = RecoverStation.create(AnvilCraft.CONFIG.storageRecoverMaxSize);
    }

    private Storages(Map<UUID, BaseStorage<?>> storages, RecoverStation<BaseStorage<?>> recover) {
        this.storages.putAll(storages);
        this.recover = recover;
    }

    public static Storages get() {
        if (!Util.isServer()) {
            return Storages.CLIENT_COPY;
        }

        // 加载期间解码存储内容会触发 onContentsChanged → get()，此时实例尚未注册进
        // DimensionDataStorage 缓存，直接 computeIfAbsent 会再次进入 load 造成无限递归。
        Storages inProgress = Storages.loading;
        if (inProgress != null) {
            return inProgress;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Cannot access Storages when the server was not initialized");
        }

        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(Storages.TYPE, "storages");
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

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag storagesTag = new CompoundTag();
        for (Map.Entry<UUID, BaseStorage<?>> entry : this.storages.entrySet()) {
            storagesTag.put(entry.getKey().toString(), entry.getValue().serializeNBT(registries));
        }
        tag.put("storages", storagesTag);
        tag.put("recover", this.recover.serializeNBT(storage -> storage == null ? new CompoundTag() : storage.serializeNBT(registries)));
        return tag;
    }

    public static Storages load(CompoundTag tag, HolderLookup.Provider registries) {
        Storages previous = Storages.loading;
        Storages.loading = new Storages();
        try {
            RecoverStation<BaseStorage<?>> recover = RecoverStation.create(AnvilCraft.CONFIG.storageRecoverMaxSize);
            if (tag.contains("recover", Tag.TAG_COMPOUND)) {
                recover.deserializeNBT((id, valueTag) -> BaseStorage.loadFromNbt(id, valueTag, registries), tag.getCompound("recover"));
            }
            return new Storages(BaseStorage.loadFromNbt("storages", tag, registries), recover);
        } finally {
            Storages.loading = previous;
        }
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
            T empty = Util.cast(IStorageType.find(clazz).value().newInstance(id));
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
