package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/** 一条恒星演化轨道上的控制点。 */
public record PhaseNode(
    StellarEvolutionPhase phaseId,
    float durationWeight,
    float radius,
    float temperature,
    float luminosity,
    float envelopeFraction,
    float pulsationAmplitude,
    float pulsationFrequency,
    String surfaceStyle,
    String eventProfileId
) {
    public static final Codec<PhaseNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        StellarEvolutionPhase.CODEC.fieldOf("phaseId").forGetter(PhaseNode::phaseId),
        Codec.FLOAT.fieldOf("durationWeight").forGetter(PhaseNode::durationWeight),
        Codec.FLOAT.fieldOf("radius").forGetter(PhaseNode::radius),
        Codec.FLOAT.fieldOf("temperature").forGetter(PhaseNode::temperature),
        Codec.FLOAT.fieldOf("luminosity").forGetter(PhaseNode::luminosity),
        Codec.FLOAT.fieldOf("envelopeFraction").forGetter(PhaseNode::envelopeFraction),
        Codec.FLOAT.fieldOf("pulsationAmplitude").forGetter(PhaseNode::pulsationAmplitude),
        Codec.FLOAT.fieldOf("pulsationFrequency").forGetter(PhaseNode::pulsationFrequency),
        Codec.STRING.optionalFieldOf("surfaceStyle", "default").forGetter(PhaseNode::surfaceStyle),
        Codec.STRING.optionalFieldOf("eventProfileId", "").forGetter(PhaseNode::eventProfileId)
    ).apply(instance, PhaseNode::new));

    /** 不带事件 profile 的便捷构造器。 */
    public PhaseNode(
        StellarEvolutionPhase phaseId,
        float durationWeight,
        float radius,
        float temperature,
        float luminosity,
        float envelopeFraction,
        float pulsationAmplitude,
        float pulsationFrequency,
        String surfaceStyle
    ) {
        this(
            phaseId,
            durationWeight,
            radius,
            temperature,
            luminosity,
            envelopeFraction,
            pulsationAmplitude,
            pulsationFrequency,
            surfaceStyle,
            ""
        );
    }

    public PhaseNode {
        Objects.requireNonNull(phaseId);
        surfaceStyle = surfaceStyle == null || surfaceStyle.isBlank() ? "default" : surfaceStyle;
        eventProfileId = eventProfileId == null ? "" : eventProfileId;
        durationWeight = finiteAtLeast(durationWeight, 0.001f);
        radius = finiteAtLeast(radius, 0.01f);
        temperature = finiteAtLeast(temperature, 100.0f);
        luminosity = finiteAtLeast(luminosity, 0.001f);
        envelopeFraction = clampFinite(envelopeFraction, 0.0f, 1.0f);
        pulsationAmplitude = clampFinite(pulsationAmplitude, 0.0f, 0.95f);
        pulsationFrequency = finiteAtLeast(pulsationFrequency, 0.0f);
    }

    private static float finiteAtLeast(float value, float minimum) {
        return Float.isFinite(value) ? Math.max(minimum, value) : minimum;
    }

    private static float clampFinite(float value, float min, float max) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : min;
    }

    /** 是否在此节点触发一个事件 profile。 */
    public boolean hasEventProfile() {
        return !eventProfileId.isBlank();
    }

    /** 未写入玩法质量字段的失质量预算，按包层剩余比例派生。 */
    public float massLossBudget() {
        return Math.clamp(1.0f - envelopeFraction, 0.0f, 1.0f);
    }
}
