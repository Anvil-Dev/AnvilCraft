package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/** 每个初始质量档位的版本、发现入口和明确终局。 */
public record StellarTrackDefinition(
    int version, int massAnvils, Map<String, String> startingNodes,
    StellarTerminal terminal, StellarMetallicity metallicity
) {
    public static final Codec<StellarTrackDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(3, 3).fieldOf("version").forGetter(StellarTrackDefinition::version),
        Codec.intRange(41, 64).fieldOf("massAnvils").forGetter(StellarTrackDefinition::massAnvils),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("startingNodes")
            .forGetter(StellarTrackDefinition::startingNodes),
        StellarTerminal.CODEC.fieldOf("terminal").forGetter(StellarTrackDefinition::terminal),
        StellarMetallicity.CODEC.optionalFieldOf("metallicity", StellarMetallicity.DEFAULT)
            .forGetter(StellarTrackDefinition::metallicity)
    ).apply(instance, StellarTrackDefinition::new));

    public StellarTrackDefinition {
        startingNodes = Map.copyOf(startingNodes);
    }
}
