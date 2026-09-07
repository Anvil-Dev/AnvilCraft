package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 各节点独立的有界响应；重复次数在启动时确定并写入快照。 */
public record StellarMetallicityResponse(
    float duration, float radius, float temperature, float luminosity, float wind, float amplitude,
    int minimumPulses, int maximumPulses
) {
    public static final StellarMetallicityResponse NONE = new StellarMetallicityResponse(0, 0, 0, 0, 0, 0, 1, 1);
    public static final Codec<StellarMetallicityResponse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.floatRange(-0.5f, 0.5f).optionalFieldOf("duration", 0.0f).forGetter(StellarMetallicityResponse::duration),
        Codec.floatRange(-0.5f, 0.5f).optionalFieldOf("radius", 0.0f).forGetter(StellarMetallicityResponse::radius),
        Codec.floatRange(-0.5f, 0.5f).optionalFieldOf("temperature", 0.0f).forGetter(StellarMetallicityResponse::temperature),
        Codec.floatRange(-0.5f, 0.5f).optionalFieldOf("luminosity", 0.0f).forGetter(StellarMetallicityResponse::luminosity),
        Codec.floatRange(-0.5f, 0.5f).optionalFieldOf("wind", 0.0f).forGetter(StellarMetallicityResponse::wind),
        Codec.floatRange(-0.5f, 0.5f).optionalFieldOf("amplitude", 0.0f).forGetter(StellarMetallicityResponse::amplitude),
        Codec.intRange(1, 16).optionalFieldOf("minimumPulses", 1).forGetter(StellarMetallicityResponse::minimumPulses),
        Codec.intRange(1, 16).optionalFieldOf("maximumPulses", 1).forGetter(StellarMetallicityResponse::maximumPulses)
    ).apply(instance, StellarMetallicityResponse::new));

    public StellarMetallicityResponse {
        for (float value : new float[]{duration, radius, temperature, luminosity, wind, amplitude}) {
            if (!Float.isFinite(value) || Math.abs(value) > 0.5f) throw new IllegalArgumentException("Invalid response");
        }
        if (minimumPulses < 1 || maximumPulses < minimumPulses || maximumPulses > 16) {
            throw new IllegalArgumentException("Invalid pulse bounds");
        }
    }

    public static float factor(float coefficient, double coordinate) {
        return (float) (1 + coefficient * coordinate);
    }

    public int pulses(double coordinate) {
        return Math.clamp((int) Math.round(minimumPulses + (maximumPulses - minimumPulses)
            * (coordinate + 1) * 0.5), minimumPulses, maximumPulses);
    }
}
