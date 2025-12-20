package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;

public enum ReinforcedConcreteHalf implements StringRepresentable {
    SINGLE("single"),
    TOP("top"),
    BOTTOM("bottom");

    private final String name;

    ReinforcedConcreteHalf(String name) {
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
