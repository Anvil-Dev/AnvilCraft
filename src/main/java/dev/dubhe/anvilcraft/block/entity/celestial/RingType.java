package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;

public enum RingType implements StringRepresentable {
    NONE("none"),
    WEAK("weak"),
    STRONG("strong");

    private final String name;

    RingType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static RingType fromName(String name) {
        for (RingType value : RingType.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return RingType.NONE;
    }
}
