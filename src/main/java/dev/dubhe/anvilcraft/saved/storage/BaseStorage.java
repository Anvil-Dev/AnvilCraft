package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.UUID;
import java.util.function.BiConsumer;

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
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("storage_id", this.id);
        tag.putString(BaseStorage.TYPE_KEY, StorageType.find(this).getSerializedName());
        tag.put("items", this.items.serializeNBT(provider));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("items", Tag.TAG_COMPOUND)) {
            this.items.deserializeNBT(provider, tag.getCompound("items"));
        }
    }
}