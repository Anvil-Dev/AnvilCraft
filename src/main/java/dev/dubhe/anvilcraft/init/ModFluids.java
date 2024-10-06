package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.fluid.OilFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFluids {

    public static final DeferredRegister<Fluid> REGISTER = DeferredRegister.create(
        Registries.FLUID,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<Fluid, OilFluid.Source> OIL = REGISTER
        .register(
            "oil",
            OilFluid.Source::new
        );

    public static final DeferredHolder<Fluid, OilFluid.Flowing> FLOWING_OIL = REGISTER
        .register(
            "flowing_oil",
            OilFluid.Flowing::new
        );

    public static void register(IEventBus eventBus){
        REGISTER.register(eventBus);
    }
}
