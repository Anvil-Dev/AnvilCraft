package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 一次运行中固定的事件实例；偏移量相对总起点，暂停只需平移该起点。 */
public record StellarScheduledEvent(
    String instanceId, String nodeId, String profileId, int startOffset, int endOffset,
    int shockOffset, int pulseIndex, long seed, StellarNodeDynamics.EventPolicy policy
) {
    public static final Codec<StellarScheduledEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("instanceId").forGetter(StellarScheduledEvent::instanceId),
        Codec.STRING.fieldOf("nodeId").forGetter(StellarScheduledEvent::nodeId),
        Codec.STRING.fieldOf("profileId").forGetter(StellarScheduledEvent::profileId),
        Codec.INT.fieldOf("startOffset").forGetter(StellarScheduledEvent::startOffset),
        Codec.INT.fieldOf("endOffset").forGetter(StellarScheduledEvent::endOffset),
        Codec.INT.fieldOf("shockOffset").forGetter(StellarScheduledEvent::shockOffset),
        Codec.INT.fieldOf("pulseIndex").forGetter(StellarScheduledEvent::pulseIndex),
        Codec.LONG.fieldOf("seed").forGetter(StellarScheduledEvent::seed),
        StellarNodeDynamics.EventPolicy.CODEC.fieldOf("policy").forGetter(StellarScheduledEvent::policy)
    ).apply(instance, StellarScheduledEvent::new));

    public StellarScheduledEvent {
        if (instanceId.isBlank() || nodeId.isBlank() || profileId.isBlank()
            || startOffset < 0 || endOffset <= startOffset || shockOffset < startOffset || shockOffset >= endOffset
            || pulseIndex < 0 || pulseIndex >= 16) {
            throw new IllegalArgumentException("Invalid stellar event instance");
        }
    }

    public float progress(double elapsed) {
        return (float) Math.clamp((elapsed - startOffset) / (endOffset - startOffset), 0, 1);
    }
}
