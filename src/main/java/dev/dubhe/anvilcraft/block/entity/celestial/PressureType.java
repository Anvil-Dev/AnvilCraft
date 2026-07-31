package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;

public enum PressureType implements StringRepresentable {
    GAS("gas"),
    ICE("ice");

    private final String name;

    PressureType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static PressureType fromName(String name) {
        for (PressureType value : PressureType.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return PressureType.GAS;
    }
}
