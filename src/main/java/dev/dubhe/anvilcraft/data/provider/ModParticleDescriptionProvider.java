package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModParticles;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.ParticleDescriptionProvider;

public class ModParticleDescriptionProvider extends ParticleDescriptionProvider {
    public ModParticleDescriptionProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addDescriptions() {
        this.spriteSet(ModParticles.PLASMA_JETS.get(), Identifier.withDefaultNamespace("generic"), 8, true);
        this.spriteSet(ModParticles.ANVILON_ENERGY.get(), AnvilCraft.of("anvilon_energy"));
        this.spriteSet(ModParticles.ANVILON_MASS.get(), AnvilCraft.of("anvilon_mass"));
        this.spriteSet(ModParticles.ANVILON_SPACE.get(), AnvilCraft.of("anvilon_space"));
        this.spriteSet(ModParticles.ANVILON_TIME.get(), AnvilCraft.of("anvilon_time"));
        this.spriteSet(ModParticles.IONOCRAFT_BACKPACK_EXHAUST.get(), Identifier.withDefaultNamespace("generic"), 8, true);
        this.spriteSet(ModParticles.OVERSEER_TRAIL.get(), Identifier.withDefaultNamespace("generic"), 8, true);
    }
}
