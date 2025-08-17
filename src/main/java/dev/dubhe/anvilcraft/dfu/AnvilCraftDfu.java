package dev.dubhe.anvilcraft.dfu;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixerBuilder;
import net.neoforged.fml.ModWorkManager;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AnvilCraftDfu {
    public static final int DATA_VERSION = 0;
    private static final Set<DSL.TypeReference> REFERENCES;
    private static final DataFixerBuilder.Result DFU = construct();

    static {
        ImmutableSet.Builder<DSL.TypeReference> builder = ImmutableSet.builder();
        REFERENCES = builder.build();
    }

    public static DataFixerBuilder.Result construct() {
        DataFixerBuilder builder = new DataFixerBuilder(DATA_VERSION);
        addFixers(builder);
        return builder.build();
    }

    public static void constructAndOptimize() {
        ProgressMeter meter = StartupNotificationManager.prependProgressBar("AnvilCraft DFU", 0);
        Executor exec = ModWorkManager.parallelExecutor();
        CompletableFuture<?> result = DFU.optimize(REFERENCES, exec);
        result.join();
        StartupNotificationManager.popBar(meter);
    }

    public static void addFixers(DataFixerBuilder builder) {

    }
}
