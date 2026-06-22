package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull String getSerializedName() {
        return name;
    }

    public static Temperature fromName(String name) {
        for (Temperature value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return MILD;
    }
}
