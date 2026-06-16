package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum CelestialBodyType implements StringRepresentable {
    ROCKY_PLANET("rocky_planet"),
    GIANT_PLANET("giant_planet"),
    STAR("star"),
    SPECIAL("special");

    private final String name;

    CelestialBodyType(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public static CelestialBodyType fromName(String name) {
        for (CelestialBodyType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return ROCKY_PLANET;
    }
}
