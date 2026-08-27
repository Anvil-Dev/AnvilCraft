package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

@Getter
public abstract class BaseStorage<T extends UnlimitedItemStacksResourceHandler> implements INBTSerializable<CompoundTag> {
    public static final String TYPE_KEY = "type";
    private final UUID id;
    private final T items = this.constructItemHandler(this::onContentsChanged);

    protected BaseStorage(UUID id) {
        this.id = id;
    }

    protected abstract T constructItemHandler(
        BiConsumer<Integer, UnlimitedItemStack> onContentsChanged
    );

    protected void onContentsChanged(int index, UnlimitedItemStack original) {
        StorageServerStub.onContentsChanged(this.id);
        Storages.get().setDirty();
    }

    protected <S extends BaseStorage<?>> S sync(T items) {
        this.items.sync(items);
        return Util.cast(this);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("storage_id", this.id);
        IStorageType.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this.getType())
            .ifSuccess(type -> tag.put(BaseStorage.TYPE_KEY, type));
        tag.put("items", this.items.serializeNBT(registries));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("items", Tag.TAG_COMPOUND)) {
            this.items.deserializeNBT(provider, tag.getCompound("items"));
        }
    }

    public abstract Holder<IStorageType<?>> getTypeHolder();

    public IStorageType<?> getType() {
        return this.getTypeHolder().value();
    }

    public static Map<UUID, BaseStorage<?>> loadFromNbt(String key, CompoundTag tag, HolderLookup.Provider registries) {
        Map<UUID, BaseStorage<?>> storages;
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            storages = BaseStorage.loadFromNbt(tag.getCompound(key), registries);
        } else {
            storages = new HashMap<>();
        }
        return storages;
    }

    public static Map<UUID, BaseStorage<?>> loadFromNbt(CompoundTag tag, HolderLookup.Provider registries) {
        Map<UUID, BaseStorage<?>> storages = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            CompoundTag entryTag = tag.getCompound(key);
            if (!entryTag.contains(BaseStorage.TYPE_KEY, Tag.TAG_STRING)) continue;
            UUID id = UUID.fromString(key);
            BaseStorage<?> storage = BaseStorage.loadFromNbt(id, entryTag, registries);
            if (storage != null) {
                storages.put(id, storage);
            }
        }
        return storages;
    }

    public static @Nullable BaseStorage<?> loadFromNbt(UUID id, CompoundTag tag, HolderLookup.Provider registries) {
        return IStorageType.CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(BaseStorage.TYPE_KEY))
            .result()
            .map(Pair::getFirst)
            .map(type -> {
                BaseStorage<?> storage = type.newInstance(id);
                storage.deserializeNBT(registries, tag);
                return storage;
            })
            .orElse(null);
    }
}
