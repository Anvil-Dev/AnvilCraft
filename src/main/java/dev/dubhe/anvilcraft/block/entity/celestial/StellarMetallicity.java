package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Random;

/** 相对太阳金属度采用有界对数分布，独立于天体外观随机流。 */
public record StellarMetallicity(double minimum, double maximum, double solarZ) {
    public static final StellarMetallicity DEFAULT = new StellarMetallicity(0.1, 2.5, 0.014);
    public static final Codec<StellarMetallicity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.doubleRange(0.001, 10).optionalFieldOf("minimum", 0.1).forGetter(StellarMetallicity::minimum),
        Codec.doubleRange(0.001, 10).optionalFieldOf("maximum", 2.5).forGetter(StellarMetallicity::maximum),
        Codec.doubleRange(0.001, 0.1).optionalFieldOf("solarZ", 0.014).forGetter(StellarMetallicity::solarZ)
    ).apply(instance, StellarMetallicity::new));

    public StellarMetallicity {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || !Double.isFinite(solarZ)
            || minimum <= 0 || maximum <= minimum || solarZ <= 0) {
            throw new IllegalArgumentException("Invalid metallicity distribution");
        }
    }

    public double sample(long seed) {
        return at(new Random(seed ^ 0x6A09E667F3BCC909L).nextDouble() * 2 - 1);
    }

    public double at(double coordinate) {
        return solarZ * Math.exp(Math.log(minimum) + (coordinate + 1) * 0.5 * Math.log(maximum / minimum));
    }

    public double coordinate(double metallicityZ) {
        return Math.clamp(2 * Math.log(metallicityZ / solarZ / minimum) / Math.log(maximum / minimum) - 1, -1, 1);
    }
}
