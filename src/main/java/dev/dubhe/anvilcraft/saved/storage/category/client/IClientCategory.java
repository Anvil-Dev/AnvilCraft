package dev.dubhe.anvilcraft.saved.storage.category.client;

import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;

public interface IClientCategory extends ICategory {
    @Override
    Type<? extends IClientCategory> getType();

    boolean testClient(UnlimitedItemStack stack);

    @Override
    default boolean test(UnlimitedItemStack stack) {
        return Util.isClient() && this.testClient(stack);
    }
}
