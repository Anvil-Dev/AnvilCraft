package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.item.ICannotFitInStationItem;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.function.BiConsumer;

public class ShulkerContainerStorage extends BaseStorage<TypeLimitItemStacksResourceHandler> {
    public static final int DEFAULT_TYPE_LIMIT = 1024;

    public ShulkerContainerStorage(UUID id) {
        super(id);
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler(BiConsumer<Integer, UnlimitedItemStack> onContentsChanged) {
        return new TypeLimitItemStacksResourceHandler(ShulkerContainerStorage.DEFAULT_TYPE_LIMIT, 65536) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !(stack.getItem() instanceof ICannotFitInStationItem) && super.isItemValid(slot, stack);
            }

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
