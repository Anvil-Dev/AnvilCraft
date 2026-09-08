package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 归一化阶段内的控制点，温度上下限同时约束金属度响应。 */
public record StellarControlPoint(
    float progress, float radius, float temperature, float luminosity, float envelope,
    float minimumTemperature, float maximumTemperature
) {
    public static final Codec<StellarControlPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.floatRange(0, 1).fieldOf("progress").forGetter(StellarControlPoint::progress),
        Codec.floatRange(0.001f, 10000).fieldOf("radius").forGetter(StellarControlPoint::radius),
        Codec.floatRange(100, 200000).fieldOf("temperature").forGetter(StellarControlPoint::temperature),
        Codec.floatRange(0.0001f, 10000000).fieldOf("luminosity").forGetter(StellarControlPoint::luminosity),
        Codec.floatRange(0, 1).fieldOf("envelope").forGetter(StellarControlPoint::envelope),
        Codec.floatRange(100, 200000).optionalFieldOf("minimumTemperature", 100.0f)
            .forGetter(StellarControlPoint::minimumTemperature),
        Codec.floatRange(100, 200000).optionalFieldOf("maximumTemperature", 200000.0f)
            .forGetter(StellarControlPoint::maximumTemperature)
    ).apply(instance, StellarControlPoint::new));

    public StellarControlPoint {
        if (!Float.isFinite(progress) || progress < 0 || progress > 1
            || !Float.isFinite(radius) || radius <= 0 || radius > 10000
            || !Float.isFinite(temperature) || temperature < minimumTemperature || temperature > maximumTemperature
            || !Float.isFinite(minimumTemperature) || !Float.isFinite(maximumTemperature)
            || minimumTemperature < 100 || maximumTemperature > 200000
            || !Float.isFinite(luminosity) || luminosity <= 0 || luminosity > 10000000
            || !Float.isFinite(envelope) || envelope < 0 || envelope > 1) {
            throw new IllegalArgumentException("Invalid stellar control point");
        }
    }

    public StellarControlPoint resolve(StellarMetallicityResponse response, double coordinate) {
        return new StellarControlPoint(progress,
            radius * StellarMetallicityResponse.factor(response.radius(), coordinate),
            Math.clamp(temperature * StellarMetallicityResponse.factor(response.temperature(), coordinate),
                minimumTemperature, maximumTemperature),
            luminosity * StellarMetallicityResponse.factor(response.luminosity(), coordinate),
            (float) Math.pow(envelope, StellarMetallicityResponse.factor(response.wind(), coordinate)),
            minimumTemperature, maximumTemperature);
    }

    public StellarVisualState visual(PhaseNode node) {
        return new StellarVisualState(radius, temperature, luminosity,
            StellarVisualState.colorForTemperature(temperature), Math.max(0.05f, (float) Math.sqrt(luminosity)),
            envelope, radius, 0, node.pulsationAmplitude(), node.dynamics().pulses(), node.surfaceStyle());
    }
}
