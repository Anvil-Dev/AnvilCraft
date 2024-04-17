package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum Half implements StringRepresentable {
    TOP("top"),
    MID("mid"),
    BOTTOM("bottom");


    private final String name;

    Half(String name) {
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
