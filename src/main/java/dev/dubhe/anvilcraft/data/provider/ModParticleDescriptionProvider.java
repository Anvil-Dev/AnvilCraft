package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModParticles;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;

public class ModParticleDescriptionProvider extends ParticleDescriptionProvider {
    public ModParticleDescriptionProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, fileHelper);
    }

    @Override
    protected void addDescriptions() {
        spriteSet(ModParticles.PLASMA_JETS.get(), ResourceLocation.withDefaultNamespace("generic"), 8, true);
        spriteSet(ModParticles.ANVILON_ENERGY.get(), AnvilCraft.of("anvilon_energy"));
        spriteSet(ModParticles.ANVILON_MASS.get(), AnvilCraft.of("anvilon_mass"));
        spriteSet(ModParticles.ANVILON_SPACE.get(), AnvilCraft.of("anvilon_space"));
        spriteSet(ModParticles.ANVILON_TIME.get(), AnvilCraft.of("anvilon_time"));
        spriteSet(ModParticles.IONOCRAFT_BACKPACK_EXHAUST.get(), AnvilCraft.of("anvilon_space"));
    }
}
