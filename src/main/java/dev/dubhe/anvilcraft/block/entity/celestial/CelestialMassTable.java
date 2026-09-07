package dev.dubhe.anvilcraft.block.entity.celestial;

import java.util.List;

/** 菜单与恒星演化共用的离散质量基准；显示偏移不参与物理计算。 */
public final class CelestialMassTable {
    public enum Unit {
        EARTH("M⊕"), SOLAR("M☉");

        private final String symbol;

        Unit(String symbol) {
            this.symbol = symbol;
        }
    }

    public record Definition(double value, Unit unit, String displayValue) {
        public String display() {
            return displayValue + " " + unit.symbol;
        }

        public double solarMass() {
            return unit == Unit.SOLAR ? value : value / 332946.0;
        }
    }

    private static final List<Definition> VALUES = List.of(
        new Definition(0.022, Unit.EARTH, "0.022"),
        new Definition(0.031, Unit.EARTH, "0.031"),
        new Definition(0.044, Unit.EARTH, "0.044"),
        new Definition(0.063, Unit.EARTH, "0.063"),
        new Definition(0.088, Unit.EARTH, "0.088"),
        new Definition(0.125, Unit.EARTH, "0.125"),
        new Definition(0.177, Unit.EARTH, "0.177"),
        new Definition(0.25, Unit.EARTH, "0.25"),
        new Definition(0.35, Unit.EARTH, "0.35"),
        new Definition(0.5, Unit.EARTH, "0.5"),
        new Definition(0.7, Unit.EARTH, "0.7"),
        new Definition(1.0, Unit.EARTH, "1"),
        new Definition(1.41, Unit.EARTH, "1.41"),
        new Definition(2.0, Unit.EARTH, "2"),
        new Definition(2.82, Unit.EARTH, "2.82"),
        new Definition(4.0, Unit.EARTH, "4"),
        new Definition(5.66, Unit.EARTH, "5.66"),
        new Definition(8.0, Unit.EARTH, "8"),
        new Definition(11.3, Unit.EARTH, "11.3"),
        new Definition(16.0, Unit.EARTH, "16"),
        new Definition(22.6, Unit.EARTH, "22.6"),
        new Definition(32.0, Unit.EARTH, "32"),
        new Definition(45.3, Unit.EARTH, "45.3"),
        new Definition(64.0, Unit.EARTH, "64"),
        new Definition(90.5, Unit.EARTH, "90.5"),
        new Definition(128.0, Unit.EARTH, "128"),
        new Definition(181.0, Unit.EARTH, "181"),
        new Definition(256.0, Unit.EARTH, "256"),
        new Definition(362.0, Unit.EARTH, "362"),
        new Definition(512.0, Unit.EARTH, "512"),
        new Definition(724.0, Unit.EARTH, "724"),
        new Definition(1000.0, Unit.EARTH, "1k"),
        new Definition(1410.0, Unit.EARTH, "1.41k"),
        new Definition(2000.0, Unit.EARTH, "2k"),
        new Definition(2820.0, Unit.EARTH, "2.82k"),
        new Definition(4000.0, Unit.EARTH, "4k"),
        new Definition(5660.0, Unit.EARTH, "5.66k"),
        new Definition(8000.0, Unit.EARTH, "8k"),
        new Definition(11300.0, Unit.EARTH, "11.3k"),
        new Definition(16000.0, Unit.EARTH, "16k"),
        new Definition(0.063, Unit.SOLAR, "0.063"),
        new Definition(0.088, Unit.SOLAR, "0.088"),
        new Definition(0.125, Unit.SOLAR, "0.125"),
        new Definition(0.177, Unit.SOLAR, "0.177"),
        new Definition(0.25, Unit.SOLAR, "0.25"),
        new Definition(0.35, Unit.SOLAR, "0.35"),
        new Definition(0.5, Unit.SOLAR, "0.5"),
        new Definition(0.7, Unit.SOLAR, "0.7"),
        new Definition(1.0, Unit.SOLAR, "1"),
        new Definition(1.41, Unit.SOLAR, "1.41"),
        new Definition(2.0, Unit.SOLAR, "2"),
        new Definition(2.82, Unit.SOLAR, "2.82"),
        new Definition(4.0, Unit.SOLAR, "4"),
        new Definition(5.66, Unit.SOLAR, "5.66"),
        new Definition(8.0, Unit.SOLAR, "8"),
        new Definition(11.3, Unit.SOLAR, "11.3"),
        new Definition(16.0, Unit.SOLAR, "16"),
        new Definition(22.6, Unit.SOLAR, "22.6"),
        new Definition(32.0, Unit.SOLAR, "32"),
        new Definition(45.3, Unit.SOLAR, "45.3"),
        new Definition(64.0, Unit.SOLAR, "64"),
        new Definition(90.5, Unit.SOLAR, "90.5"),
        new Definition(128.0, Unit.SOLAR, "128"),
        new Definition(181.0, Unit.SOLAR, "181")
    );

    private CelestialMassTable() {
    }

    public static Definition at(int anvils) {
        if (anvils < 1 || anvils > VALUES.size()) throw new IllegalArgumentException("Invalid mass anvils: " + anvils);
        return VALUES.get(anvils - 1);
    }

    public static String display(int anvils) {
        return anvils >= 1 && anvils <= VALUES.size() ? at(anvils).display() : "---";
    }
}
