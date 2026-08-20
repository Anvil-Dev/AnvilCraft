package dev.dubhe.anvilcraft.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

import java.util.List;
import java.util.stream.Stream;

/** Vanilla overworld climate layout with access to the original biome data. */
public class VanillaOverworldBiomeSource extends BiomeSource {
    public static final MapCodec<VanillaOverworldBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(RegistryOps.retrieveGetter(Registries.BIOME))
            .apply(instance, VanillaOverworldBiomeSource::new)
    );

    private final MultiNoiseBiomeSource delegate;

    public VanillaOverworldBiomeSource(HolderGetter<Biome> biomes) {
        MultiNoiseBiomeSourceParameterList parameters = new MultiNoiseBiomeSourceParameterList(
            MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD, biomes
        );
        delegate = MultiNoiseBiomeSource.createFromList(parameters.parameters());
    }

    public static BiomeGenerationSettings originalGenerationSettings(Holder<Biome> biome) {
        return biome.value().modifiableBiomeInfo().getOriginalBiomeInfo().generationSettings();
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return delegate.possibleBiomes().stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return delegate.getNoiseBiome(x, y, z, sampler);
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, Climate.Sampler sampler) {
        delegate.addDebugInfo(info, pos, sampler);
    }
}
