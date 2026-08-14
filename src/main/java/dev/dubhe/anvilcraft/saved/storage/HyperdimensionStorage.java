package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;

import java.util.UUID;
import java.util.function.BiConsumer;

public class HyperdimensionStorage extends BaseStorage<UnlimitedItemStacksResourceHandler> {
    public HyperdimensionStorage(UUID id) {
        super(id);
    }

    @Override
    protected UnlimitedItemStacksResourceHandler constructItemHandler(BiConsumer<Integer, UnlimitedItemStack> onContentsChanged) {
        return new UnlimitedItemStacksResourceHandler(65536) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }
}