package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** 新格式节点的曲线、脉冲与玩法策略。 */
public record StellarNodeDynamics(
    List<StellarControlPoint> points, StellarMetallicityResponse response,
    EventPolicy eventPolicy, int pulses, float wind
) {
    public static final StellarNodeDynamics LEGACY = new StellarNodeDynamics(
        List.of(), StellarMetallicityResponse.NONE, EventPolicy.VISUAL, 1, 0);
    public static final Codec<StellarNodeDynamics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        StellarControlPoint.CODEC.listOf().fieldOf("points").forGetter(StellarNodeDynamics::points),
        StellarMetallicityResponse.CODEC.optionalFieldOf("response", StellarMetallicityResponse.NONE)
            .forGetter(StellarNodeDynamics::response),
        EventPolicy.CODEC.optionalFieldOf("eventPolicy", EventPolicy.VISUAL).forGetter(StellarNodeDynamics::eventPolicy),
        Codec.intRange(1, 16).optionalFieldOf("pulses", 1).forGetter(StellarNodeDynamics::pulses),
        Codec.floatRange(0, 1).optionalFieldOf("wind", 0.0f).forGetter(StellarNodeDynamics::wind)
    ).apply(instance, StellarNodeDynamics::new));

    public enum EventPolicy {
        VISUAL, SUPERNOVA, DIRECT_COLLAPSE, PPISN, PISN;

        public static final Codec<EventPolicy> CODEC = Codec.STRING.comapFlatMap(id -> {
            for (EventPolicy policy : values()) {
                if (policy.name().equalsIgnoreCase(id)) return DataResult.success(policy);
            }
            return DataResult.error(() -> "Unknown stellar event policy: " + id);
        }, policy -> policy.name().toLowerCase(java.util.Locale.ROOT));

        public boolean destructive() {
            return this == SUPERNOVA || this == PISN;
        }
    }

    public StellarNodeDynamics {
        points = List.copyOf(points);
        if (pulses < 1 || pulses > 16 || !Float.isFinite(wind) || wind < 0 || wind > 1) {
            throw new IllegalArgumentException("Invalid stellar dynamics");
        }
        if (!points.isEmpty()) {
            if (points.size() < 2 || points.getFirst().progress() != 0 || points.getLast().progress() != 1) {
                throw new IllegalArgumentException("Stellar curves must cover [0, 1]");
            }
            float previous = -1;
            for (StellarControlPoint point : points) {
                if (point.progress() <= previous) throw new IllegalArgumentException("Unordered stellar control points");
                previous = point.progress();
            }
        }
    }

    public StellarNodeDynamics resolve(double coordinate) {
        return new StellarNodeDynamics(points.stream().map(point -> point.resolve(response, coordinate)).toList(),
            StellarMetallicityResponse.NONE, eventPolicy, response.pulses(coordinate),
            Math.clamp(wind * StellarMetallicityResponse.factor(response.wind(), coordinate), 0, 1));
    }

    public StellarVisualState sample(PhaseNode node, PhaseNode previous, float progress) {
        if (points.isEmpty()) {
            return StellarVisualState.interpolate(StellarTrack.visualForNode(previous),
                StellarTrack.visualForNode(node), progress);
        }
        for (int index = 1; index < points.size(); index++) {
            StellarControlPoint right = points.get(index);
            if (progress <= right.progress() || index == points.size() - 1) {
                StellarControlPoint left = points.get(index - 1);
                return StellarVisualState.interpolate(left.visual(node), right.visual(node),
                    (progress - left.progress()) / (right.progress() - left.progress()));
            }
        }
        return points.getLast().visual(node);
    }
}
