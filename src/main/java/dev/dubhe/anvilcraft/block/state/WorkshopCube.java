package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;

public enum WorkshopCube implements StringRepresentable {
    CORNER("corner"),
    CENTER("center");

    private final String name;

    WorkshopCube(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}