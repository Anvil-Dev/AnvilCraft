package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 终局与视觉事件分别配置；质量坐标与太阳质量必须指向同一基准值。 */
public record StellarTerminal(Kind kind, int massAnvils, double solarMass, int size) {
    public static final Codec<StellarTerminal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Kind.CODEC.fieldOf("kind").forGetter(StellarTerminal::kind),
        Codec.intRange(0, 64).fieldOf("massAnvils").forGetter(StellarTerminal::massAnvils),
        Codec.doubleRange(0, 181).fieldOf("solarMass").forGetter(StellarTerminal::solarMass),
        Codec.intRange(1, 64).fieldOf("size").forGetter(StellarTerminal::size)
    ).apply(instance, StellarTerminal::new));

    public enum Kind {
        WHITE_DWARF("white_dwarf"), NEUTRON_STAR("neutron_star"), BLACK_HOLE("black_hole"),
        NONE("disruption"), KEEP("keep");

        public static final Codec<Kind> CODEC = Codec.STRING.comapFlatMap(serializedId -> {
            for (Kind kind : values()) {
                if (kind.id.equals(serializedId)) return DataResult.success(kind);
            }
            return DataResult.error(() -> "Unknown stellar terminal: " + serializedId);
        }, Kind::id);
        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }
    }

    public void validate(double initialSolarMass) {
        if (!Double.isFinite(this.solarMass) || this.solarMass < 0 || this.solarMass > initialSolarMass) {
            throw new IllegalArgumentException("Remnant mass exceeds initial mass");
        }
        if (this.kind == Kind.NONE) {
            if (this.massAnvils != 0 || this.solarMass != 0) throw new IllegalArgumentException("Disruption has a remnant");
        } else if (this.massAnvils < 1 || Math.abs(CelestialMassTable.at(this.massAnvils).solarMass() - this.solarMass) > 1e-8) {
            throw new IllegalArgumentException("Remnant mass and menu coordinate disagree");
        }
    }
}
