package dev.dubhe.anvilcraft.init;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.VanillaOverworldBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Registers codec types used by AnvilCraft's built-in dimensions. */
public final class ModBiomeSources {
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> REGISTER = DeferredRegister.create(
        Registries.BIOME_SOURCE, AnvilCraft.MOD_ID
    );

    public static final Supplier<MapCodec<? extends BiomeSource>> VANILLA_OVERWORLD = REGISTER.register(
        "vanilla_overworld", () -> VanillaOverworldBiomeSource.CODEC
    );

    private ModBiomeSources() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
