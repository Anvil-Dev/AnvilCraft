package dev.dubhe.anvilcraft.api.sc.category.client;

import dev.dubhe.anvilcraft.api.sc.category.ICategory;
import dev.dubhe.anvilcraft.util.DistExecutor;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;

import java.util.concurrent.atomic.AtomicBoolean;

public interface IClientCategory extends ICategory {
    @OnlyIn(Dist.CLIENT)
    boolean testClient(UnlimitedItemStack stack);

    @Override
    default boolean test(UnlimitedItemStack stack) {
        if (!FMLLoader.getDist().isClient()) return false;
        AtomicBoolean result = new AtomicBoolean(false);
        DistExecutor.run(Dist.CLIENT, () -> () -> result.set(this.testClient(stack)));
        return result.get();
    }
}
