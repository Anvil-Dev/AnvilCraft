package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 游戏质量砧子到近似物理质量族的映射。 */
public record StellarMassBand(
    String id,
    int minGameMass,
    int maxGameMass,
    float minSolarMass,
    float maxSolarMass,
    String trackFamily,
    String defaultTerminalProfile
) {
    public static final Codec<StellarMassBand> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("id").forGetter(StellarMassBand::id),
        Codec.INT.fieldOf("minGameMass").forGetter(StellarMassBand::minGameMass),
        Codec.INT.fieldOf("maxGameMass").forGetter(StellarMassBand::maxGameMass),
        Codec.FLOAT.fieldOf("minSolarMass").forGetter(StellarMassBand::minSolarMass),
        Codec.FLOAT.fieldOf("maxSolarMass").forGetter(StellarMassBand::maxSolarMass),
        Codec.STRING.fieldOf("trackFamily").forGetter(StellarMassBand::trackFamily),
        Codec.STRING.fieldOf("defaultTerminalProfile").forGetter(StellarMassBand::defaultTerminalProfile)
    ).apply(instance, StellarMassBand::new));

    public StellarMassBand {
        minGameMass = Math.clamp(minGameMass, 1, 64);
        maxGameMass = Math.clamp(maxGameMass, minGameMass, 64);
        minSolarMass = Float.isFinite(minSolarMass) ? Math.max(0.01f, minSolarMass) : 0.01f;
        maxSolarMass = Float.isFinite(maxSolarMass) ? Math.max(minSolarMass, maxSolarMass) : minSolarMass;
        trackFamily = trackFamily == null || trackFamily.isBlank() ? "low_mass" : trackFamily;
        defaultTerminalProfile = defaultTerminalProfile == null ? "" : defaultTerminalProfile;
    }

    public boolean contains(int gameMass) {
        return gameMass >= minGameMass && gameMass <= maxGameMass;
    }

    /** 返回此质量带内的归一化位置，便于在离线锚点之间插值。 */
    public float progress(int gameMass) {
        if (maxGameMass <= minGameMass) return 0.0f;
        return Math.clamp((gameMass - minGameMass) / (float) (maxGameMass - minGameMass), 0.0f, 1.0f);
    }

    public float solarMassAt(int gameMass) {
        return minSolarMass + (maxSolarMass - minSolarMass) * progress(gameMass);
    }

    public int gameMassMin() {
        return minGameMass;
    }

    public int gameMassMax() {
        return maxGameMass;
    }

    public float solarMassMin() {
        return minSolarMass;
    }

    public float solarMassMax() {
        return maxSolarMass;
    }
}
