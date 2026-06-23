package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum PressureType implements StringRepresentable {
    GAS("gas"),
    ICE("ice");

    private final String name;

    PressureType(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public static PressureType fromName(String name) {
        for (PressureType value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return GAS;
    }
}
