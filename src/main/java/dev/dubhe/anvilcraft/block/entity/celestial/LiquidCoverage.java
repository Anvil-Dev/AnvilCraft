package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;

public enum LiquidCoverage implements StringRepresentable {
    NONE("none"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String name;

    LiquidCoverage(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static LiquidCoverage fromName(String name) {
        for (LiquidCoverage value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return NONE;
    }
}
