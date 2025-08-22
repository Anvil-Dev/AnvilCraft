package dev.dubhe.anvilcraft.init.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.CraterConfiguration;
import dev.dubhe.anvilcraft.worldgen.CraterFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFeatures {
    private static final DeferredRegister<Feature<?>> REGISTER = DeferredRegister.create(Registries.FEATURE, AnvilCraft.MOD_ID);

    public static final DeferredHolder<Feature<?>, CraterFeature> CRATER = REGISTER.register("crater", () -> new CraterFeature(
        CraterConfiguration.CODEC
    ));

    public static void initialize(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
