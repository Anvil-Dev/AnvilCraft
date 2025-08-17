package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticles {
    private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, AnvilCraft.MOD_ID);

    public static final Supplier<SimpleParticleType> PLASMA_JETS = REGISTER.register(
        "plasma_jets", () -> new SimpleParticleType(false)
    );

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
