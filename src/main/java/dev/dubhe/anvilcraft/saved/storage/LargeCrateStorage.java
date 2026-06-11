package dev.dubhe.anvilcraft.saved.storage;

import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;

public class LargeCrateStorage extends BaseStorage {
    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler() {
        return new TypeLimitItemStacksResourceHandler(65536);
    }
}
