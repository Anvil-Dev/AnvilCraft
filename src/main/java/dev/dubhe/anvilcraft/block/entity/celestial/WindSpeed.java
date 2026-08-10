package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;

public enum WindSpeed implements StringRepresentable {
    HIGH("high"),
    VERY_HIGH("very_high");

    private final String name;

    WindSpeed(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static WindSpeed fromName(String name) {
        for (WindSpeed value : WindSpeed.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return WindSpeed.HIGH;
    }
}
