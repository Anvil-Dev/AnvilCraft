package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum WindSpeed implements StringRepresentable {
    HIGH("high"),
    VERY_HIGH("very_high");

    private final String name;

    WindSpeed(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public static WindSpeed fromName(String name) {
        for (WindSpeed value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return HIGH;
    }
}
