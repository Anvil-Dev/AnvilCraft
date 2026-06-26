package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.recover.RecoverStation;
import lombok.Getter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
public class Storages extends SavedData {
    public static final Identifier ID = AnvilCraft.of("storages");
    public static final MapCodec<Storages> CODEC = CodecUtil.mapCodec(
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, BaseStorage.CODEC.codec())
            .fieldOf("storages")
            .forGetter(Storages::getStorages),
        RecoverStation.codec(BaseStorage.CODEC)
            .fieldOf("recover")
            .forGetter(Storages::getRecover),
        Storages::new
    );
    public static final SavedDataType<Storages> TYPE = new SavedDataType<>(Storages.ID, Storages::new, Storages.CODEC.codec());
    public static final Storages CLIENT_COPY = new Storages();
    private final Map<UUID, BaseStorage> storages;
    private final RecoverStation<BaseStorage> recover;

    private Storages() {
        this(new HashMap<>(), RecoverStation.create(AnvilCraft.CONFIG.storageRecoverMaxSize));
    }

    private Storages(Map<UUID, BaseStorage> storages, RecoverStation<BaseStorage> recover) {
        this.storages = new HashMap<>(storages);
        this.recover = recover;
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
        SavedDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(Storages.TYPE);
    }

    public Optional<BaseStorage> get(UUID id) {
        return Optional.ofNullable(this.storages.get(id));
    }

    public <T extends BaseStorage> Optional<T> get(UUID id, Class<T> clazz) {
        return Util.castSafely(this.storages.get(id), clazz);
    }

    public <T extends BaseStorage> T getOrCreate(UUID id, Class<T> clazz) {
        BaseStorage storage = this.storages.get(id);
        if (storage == null) {
            T empty = Util.cast(StorageType.find(clazz).newInstance(id));
            this.storages.put(id, empty);
            return empty;
        }
        return Util.castSafely(storage, clazz).orElseThrow(() -> new IllegalArgumentException(
            "Storage with id '%s' cannot be cast to expected type '%s'. Actual type: %s"
                .formatted(id, clazz.getName(), storage.getClass().getName())
        ));
    }
}
