package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

@Getter
public abstract class BaseStorage<T extends UnlimitedItemStacksResourceHandler> {
    public static final MapCodec<BaseStorage<?>> CODEC = StorageType.CODEC
        .dispatchMap(StorageType::find, StorageType::codec);
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseStorage<?>> STREAM_CODEC = StorageType.STREAM_CODEC
        .<RegistryFriendlyByteBuf>cast()
        .dispatch(StorageType::find, StorageType::streamCodec);
    private final UUID id;
    private final T items = this.constructItemHandler(this::onContentsChanged);

    protected BaseStorage(UUID id) {
        this.id = id;
    }

    protected abstract T constructItemHandler(
        IntObjectBiConsumer<UnlimitedItemStack> onContentsChanged
    );

    protected void onContentsChanged(int index, UnlimitedItemStack original) {
        StorageServerStub.onContentsChanged(this.id);
        Storages.get().setDirty();
    }

    protected <S extends BaseStorage<?>> S sync(T items) {
        this.items.sync(items);
        return Util.cast(this);
    }
}