package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum RingType implements StringRepresentable {
    NONE("none"),
    WEAK("weak"),
    STRONG("strong");

    private final String name;

    RingType(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public static RingType fromName(String name) {
        for (RingType value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return NONE;
    }
}
