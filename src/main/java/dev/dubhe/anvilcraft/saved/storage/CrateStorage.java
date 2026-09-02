package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.OverflowDisposalItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.Holder;

import java.util.UUID;
import java.util.function.BiConsumer;

public class CrateStorage extends BaseStorage<OverflowDisposalItemStacksResourceHandler> {
    public CrateStorage(UUID id) {
        super(id);
    }

    @Override
    protected OverflowDisposalItemStacksResourceHandler constructItemHandler(
        BiConsumer<Integer, UnlimitedItemStack> onContentsChanged
    ) {
        return new OverflowDisposalItemStacksResourceHandler(2048) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }

    @Override
    public Holder<IStorageType<?>> getTypeHolder() {
        return ModStorageTypes.CRATE;
    }
}
