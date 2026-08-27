package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.InfiniteItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.Holder;

import java.util.UUID;
import java.util.function.BiConsumer;

public class HyperdimensionStorage extends BaseStorage<UnlimitedItemStacksResourceHandler> {
    public HyperdimensionStorage(UUID id) {
        super(id);
    }

    @Override
    protected UnlimitedItemStacksResourceHandler constructItemHandler(BiConsumer<Integer, UnlimitedItemStack> onContentsChanged) {
        return new InfiniteItemStacksResourceHandler() {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }

    @Override
    public Holder<IStorageType<?>> getTypeHolder() {
        return ModStorageTypes.HYPERDIMENSION;
    }
}
