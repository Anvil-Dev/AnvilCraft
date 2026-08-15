package dev.dubhe.anvilcraft.saved.storage.category.client;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import net.neoforged.api.distmarker.Dist;

import java.util.concurrent.atomic.AtomicBoolean;

public interface IClientCategory extends ICategory {
    @Override
    Type<? extends IClientCategory> getType();

    default boolean testClient(UnlimitedItemStack stack) {
        return false;
    }

    @Override
    default boolean test(UnlimitedItemStack stack) {
        if (Util.isServer()) {
            return false;
        }
        AtomicBoolean result = new AtomicBoolean(false);
        DistExecutor.run(Dist.CLIENT, () -> () -> result.set(this.testClient(stack)));
        return result.get();
    }
}
