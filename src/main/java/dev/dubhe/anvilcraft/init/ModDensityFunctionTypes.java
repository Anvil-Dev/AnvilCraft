package dev.dubhe.anvilcraft.init;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.CraterDensityFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Registers density function types used by AnvilCraft's built-in dimensions. */
public final class ModDensityFunctionTypes {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> REGISTER = DeferredRegister.create(
        Registries.DENSITY_FUNCTION_TYPE, AnvilCraft.MOD_ID
    );

    public static final Supplier<MapCodec<? extends DensityFunction>> CRATER = REGISTER.register(
        "crater", () -> CraterDensityFunction.MAP_CODEC
    );

    private ModDensityFunctionTypes() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
