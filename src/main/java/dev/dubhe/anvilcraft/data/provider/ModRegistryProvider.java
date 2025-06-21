package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.worldgen.ModBiomes;
import dev.dubhe.anvilcraft.data.worldgen.ModDimensionTypes;
import dev.dubhe.anvilcraft.data.worldgen.ModDimensions;
import dev.dubhe.anvilcraft.data.worldgen.ModNoiseGeneratorSettings;
import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModRegistryProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
        .add(Registries.ENCHANTMENT, ModEnchantments::bootstrap)
        .add(Registries.DIMENSION_TYPE, ModDimensionTypes::bootstrap)
        .add(Registries.BIOME, ModBiomes::bootstrap)
        .add(Registries.NOISE_SETTINGS, ModNoiseGeneratorSettings::bootstrap)
        .add(Registries.LEVEL_STEM, ModDimensions::bootstrap);

    public ModRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(AnvilCraft.MOD_ID));
    }
}
