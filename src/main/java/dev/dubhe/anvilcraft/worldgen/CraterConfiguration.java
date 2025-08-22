package dev.dubhe.anvilcraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record CraterConfiguration(IntProvider radius, IntProvider depth) implements FeatureConfiguration {
    public static final Codec<CraterConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        IntProvider.CODEC
            .fieldOf("radius")
            .forGetter(CraterConfiguration::radius),
        IntProvider.CODEC
            .fieldOf("depth")
            .forGetter(CraterConfiguration::depth)
    ).apply(instance, CraterConfiguration::new));
}
