package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.Holder;

import java.util.UUID;
import java.util.function.BiConsumer;

public class ShulkerContainerStorage extends BaseStorage<TypeLimitItemStacksResourceHandler> {
    public ShulkerContainerStorage(UUID id) {
        super(id);
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler(BiConsumer<Integer, UnlimitedItemStack> onContentsChanged) {
        return new TypeLimitItemStacksResourceHandler(65536, 65536) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }

    @Override
    public Holder<IStorageType<?>> getTypeHolder() {
        return ModStorageTypes.SHULKER_CONTAINER;
    }
}
