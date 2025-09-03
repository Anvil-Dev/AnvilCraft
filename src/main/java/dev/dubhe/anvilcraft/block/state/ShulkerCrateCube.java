package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum ShulkerCrateCube implements StringRepresentable {
    CORNER("corner"),
    CENTER("center");

    private final String name;

    ShulkerCrateCube(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
