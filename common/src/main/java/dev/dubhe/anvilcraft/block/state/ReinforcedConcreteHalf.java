package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
