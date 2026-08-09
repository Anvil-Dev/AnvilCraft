package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;

public enum Temperature implements StringRepresentable {
    FREEZING("freezing"),
    COLD("cold"),
    MILD("mild"),
    HOT("hot"),
    SCORCHED("scorched");

    private final String name;

    Temperature(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static Temperature fromName(String name) {
        for (Temperature value : Temperature.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return Temperature.MILD;
    }
}
