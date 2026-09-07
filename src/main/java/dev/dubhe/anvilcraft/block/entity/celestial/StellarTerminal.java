package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 终局与视觉事件分别配置；质量坐标与太阳质量必须指向同一基准值。 */
public record StellarTerminal(Kind kind, int massAnvils, double solarMass, int size) {
    public static final StellarTerminal LEGACY = new StellarTerminal(Kind.KEEP, 0, 0, 1);
    public static final Codec<StellarTerminal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Kind.CODEC.fieldOf("kind").forGetter(StellarTerminal::kind),
        Codec.intRange(0, 64).fieldOf("massAnvils").forGetter(StellarTerminal::massAnvils),
        Codec.doubleRange(0, 181).fieldOf("solarMass").forGetter(StellarTerminal::solarMass),
        Codec.intRange(1, 64).fieldOf("size").forGetter(StellarTerminal::size)
    ).apply(instance, StellarTerminal::new));

    public enum Kind {
        WHITE_DWARF("white_dwarf"), NEUTRON_STAR("neutron_star"), BLACK_HOLE("black_hole"),
        NONE("disruption"), KEEP("keep");

        public static final Codec<Kind> CODEC = Codec.STRING.comapFlatMap(id -> {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) return DataResult.success(kind);
            }
            return DataResult.error(() -> "Unknown stellar terminal: " + id);
        }, Kind::id);
        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public void validate(double initialSolarMass) {
        if (!Double.isFinite(solarMass) || solarMass < 0 || solarMass > initialSolarMass) {
            throw new IllegalArgumentException("Remnant mass exceeds initial mass");
        }
        if (kind == Kind.NONE) {
            if (massAnvils != 0 || solarMass != 0) throw new IllegalArgumentException("Disruption has a remnant");
        } else if (massAnvils < 1 || Math.abs(CelestialMassTable.at(massAnvils).solarMass() - solarMass) > 1e-8) {
            throw new IllegalArgumentException("Remnant mass and menu coordinate disagree");
        }
    }
}
