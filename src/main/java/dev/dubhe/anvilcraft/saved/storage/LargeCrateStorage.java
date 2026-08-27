package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.Holder;

import java.util.UUID;
import java.util.function.BiConsumer;

public class LargeCrateStorage extends BaseStorage<SpaceSizeItemStacksResourceHandler> {
    public LargeCrateStorage(UUID id) {
        super(id);
    }

    @Override
    protected SpaceSizeItemStacksResourceHandler constructItemHandler(BiConsumer<Integer, UnlimitedItemStack> onContentsChanged) {
        return new SpaceSizeItemStacksResourceHandler(65536) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }

    @Override
    public Holder<IStorageType<?>> getTypeHolder() {
        return ModStorageTypes.LARGE_CRATE;
    }
}
