package dev.dubhe.anvilcraft.init.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.CraterConfiguration;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> MEGA_CRATER = key("mega_crater");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LARGE_CRATER = key("large_crater");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_CRATER = key("small_crater");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(
            MEGA_CRATER,
            new ConfiguredFeature<>(
                ModFeatures.CRATER.get(),
                new CraterConfiguration(UniformInt.of(32, 48), UniformInt.of(8, 16))
            )
        );
        context.register(
            LARGE_CRATER,
            new ConfiguredFeature<>(
                ModFeatures.CRATER.get(),
                new CraterConfiguration(UniformInt.of(12, 15), UniformInt.of(3, 5))
            )
        );
        context.register(
            SMALL_CRATER,
            new ConfiguredFeature<>(
                ModFeatures.CRATER.get(),
                new CraterConfiguration(UniformInt.of(4, 7), UniformInt.of(2, 3))
            )
        );
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, AnvilCraft.of(name));
    }
}
